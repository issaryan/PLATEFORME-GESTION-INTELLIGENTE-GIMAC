package cm.gimac.des.pilotage.application.usecase;

import cm.gimac.des.pilotage.application.port.in.Appelant;
import cm.gimac.des.pilotage.application.port.in.Page;
import cm.gimac.des.pilotage.application.port.in.RapportsUseCase;
import cm.gimac.des.pilotage.application.port.out.DocumentPort;
import cm.gimac.des.pilotage.application.port.out.OrganisationPort.Membre;
import cm.gimac.des.pilotage.application.port.out.OrganisationPort.Personne;
import cm.gimac.des.pilotage.application.port.out.OrganisationPort.Pouvoir;
import cm.gimac.des.pilotage.application.port.out.OrganisationPort.Rattachement;
import cm.gimac.des.pilotage.domain.activite.Activite;
import cm.gimac.des.pilotage.domain.activite.EtatRapport;
import cm.gimac.des.pilotage.domain.activite.Indicateurs;
import cm.gimac.des.pilotage.domain.activite.ModeRapport;
import cm.gimac.des.pilotage.domain.activite.Periode;
import cm.gimac.des.pilotage.domain.activite.Rapport;
import cm.gimac.des.pilotage.domain.activite.StatutActivite;
import cm.gimac.des.pilotage.domain.activite.TypeRapport;
import cm.gimac.des.pilotage.domain.event.RapportSoumisPayload;
import cm.gimac.des.pilotage.domain.event.RapportVisePayload;
import cm.gimac.des.pilotage.domain.exception.PilotageException;
import cm.gimac.des.pilotage.domain.service.RedactionRapport;
import cm.gimac.des.pilotage.domain.service.ReglesActivite;
import cm.gimac.des.pilotage.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.pilotage.repository.ActiviteRepository;
import cm.gimac.des.pilotage.repository.RapportRepository;
import cm.gimac.des.pilotage.repository.TacheRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rapports quotidiens et hebdomadaires (§2.4) : un seul par (auteur, type, période), période future
 * refusée, préremplissage, document déposé dans MS-DOCUMENT (finalité RAPPORT_ACTIVITE, ticket
 * vérifié), soumission au n+1 du moment, visa par le destinataire ou la chaîne hiérarchique
 * (PIL_TEAM_READ et RAPPORT_VISER couvrant l'auteur, D-81) ; événements pilotage.report-* par l'outbox, jamais le contenu.
 */
@Service
@Transactional
public class RapportService implements RapportsUseCase {

    static final String AGREGAT = "rapport";
    private static final List<EtatRapport> TRANSMIS = List.of(EtatRapport.SOUMIS, EtatRapport.VISE);

    private final RapportRepository rapports;
    private final ActiviteRepository activites;
    private final TacheRepository taches;
    private final Equipes equipes;
    private final DocumentPort documents;
    private final OutboxPublisher outbox;
    private final Clock horloge;

    public RapportService(
        RapportRepository rapports,
        ActiviteRepository activites,
        TacheRepository taches,
        Equipes equipes,
        DocumentPort documents,
        OutboxPublisher outbox,
        Clock horloge
    ) {
        this.rapports = rapports;
        this.activites = activites;
        this.taches = taches;
        this.equipes = equipes;
        this.documents = documents;
        this.outbox = outbox;
        this.horloge = horloge;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RapportLu> rapports(Appelant appelant, String vue, String type, String etat, Integer page, Integer taille) {
        TypeRapport t = type == null || type.isBlank() ? null : ReglesActivite.valeur("type", TypeRapport.class, type, null);
        EtatRapport e = etat == null || etat.isBlank() ? null : ReglesActivite.valeur("etat", EtatRapport.class, etat, null);
        PageRequest p = PageRequest.of(ReglesActivite.page(page), ReglesActivite.taille(taille));
        org.springframework.data.domain.Page<Rapport> r;
        switch (vue == null || vue.isBlank() ? "moi" : vue.strip()) {
            case "moi" -> r = rapports.deAuteur(appelant.id(), t, e, p);
            case "recus" -> {
                List<String> equipe = appelant.litEquipe() ? equipes.membres(appelant, Pouvoir.RAPPORT_VISER).stream().map(Membre::id).toList() : List.of();
                // Liste jamais vide dans la clause « in » : un identifiant impossible la remplace.
                r = rapports.recus(appelant.id(), equipe.isEmpty() ? List.of("") : equipe, TRANSMIS, t, e, p);
            }
            default -> throw PilotageException.champ("vue", "Vue moi ou recus attendue");
        }
        Map<String, String> noms = equipes.noms(r.getContent().stream().flatMap(RapportService::comptes).toList());
        return new Page<>(r.getContent().stream().map(x -> new RapportLu(x, indicateurs(x), noms)).toList(), r.getTotalElements(), p.getPageNumber(), p.getPageSize());
    }

    @Override
    @Transactional(readOnly = true)
    public RapportLu rapport(Appelant appelant, UUID id) {
        Rapport r = trouver(id);
        exigerLecture(appelant, r);
        return lu(r);
    }

    @Override
    @Transactional(readOnly = true)
    public String preremplir(Appelant appelant, String type, String periode) {
        Periode p = periode(type, periode);
        LocalDate suivant = ReglesActivite.jourOuvreSuivant(p.fin());
        List<RedactionRapport.Ligne> lignes = lignes(activites.findByAuteurIdAndJourBetweenOrderByJourAscOrdreAscCreeLeAsc(appelant.id(), p.debut(), p.fin()));
        List<RedactionRapport.Ligne> prevues = lignes(activites.findByAuteurIdAndJourBetweenOrderByJourAscOrdreAscCreeLeAsc(appelant.id(), suivant, suivant));
        List<RedactionRapport.TacheTerminee> terminees = taches
            .termineesEntre(appelant.id(), ReglesActivite.debutJour(p.debut()), ReglesActivite.debutJour(p.fin().plusDays(1)))
            .stream()
            .map(t -> new RedactionRapport.TacheTerminee(t.getTermineeLe().atZone(ReglesActivite.FUSEAU).toLocalDate(), t.getTitre()))
            .toList();
        return RedactionRapport.rediger(p, lignes, terminees, prevues, suivant);
    }

    @Override
    public RapportLu creer(Appelant appelant, RapportSaisi saisie) {
        if (saisie == null) {
            throw PilotageException.champ("type", "Corps de la requête absent");
        }
        Periode p = periode(saisie.type(), saisie.periode());
        if (rapports.existsByAuteurIdAndTypeAndPeriode(appelant.id(), p.type(), p.texte())) {
            throw existant(p);
        }
        Instant le = horloge.instant();
        Rapport r = new Rapport(UUID.randomUUID(), appelant.id(), p, mode(saisie.mode()), le);
        rediger(r, appelant, saisie, le);
        try {
            rapports.saveAndFlush(r);
        } catch (DataIntegrityViolationException e) {
            throw existant(p);
        }
        return lu(r);
    }

    @Override
    public RapportLu modifier(Appelant appelant, UUID id, RapportSaisi saisie) {
        Rapport r = auteur(appelant, id);
        if (saisie == null) {
            throw PilotageException.champ("mode", "Corps de la requête absent");
        }
        r.exigerBrouillon();
        rediger(r, appelant, saisie, horloge.instant());
        return lu(r);
    }

    @Override
    public RapportLu soumettre(Appelant appelant, UUID id) {
        Rapport r = auteur(appelant, id);
        r.exigerBrouillon();
        String destinataire = equipes.rattachement(appelant.id()).map(Rattachement::responsable).map(Personne::id).orElse(null);
        Instant le = horloge.instant();
        r.soumettre(destinataire, calculer(r.getAuteurId(), r.periode()), le);
        outbox.publish(
            AGREGAT,
            r.getId(),
            RapportSoumisPayload.TYPE,
            new RapportSoumisPayload(r.getId(), r.getType().name(), r.getPeriode(), r.getEtat().name(), r.getAuteurId(), destinataire, appelant.id(), le)
        );
        return lu(r);
    }

    @Override
    public RapportLu viser(Appelant appelant, UUID id, String commentaire) {
        Rapport r = trouver(id);
        boolean destinataire = appelant.id().equals(r.getDestinataireId());
        boolean chaine = !appelant.id().equals(r.getAuteurId()) && appelant.litEquipe() && equipes.couvert(appelant, Pouvoir.RAPPORT_VISER, r.getAuteurId()).isPresent();
        if (!destinataire && !chaine) {
            if (appelant.id().equals(r.getAuteurId())) {
                throw PilotageException.reservee("L'auteur ne vise pas son propre rapport");
            }
            throw PilotageException.horsEquipe();
        }
        Instant le = horloge.instant();
        r.viser(appelant.id(), commentaire == null || commentaire.isBlank() ? null : commentaire.strip(), le);
        outbox.publish(
            AGREGAT,
            r.getId(),
            RapportVisePayload.TYPE,
            new RapportVisePayload(r.getId(), r.getType().name(), r.getPeriode(), r.getEtat().name(), r.getAuteurId(), r.getDestinataireId(), appelant.id(), le)
        );
        return lu(r);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentLu document(Appelant appelant, UUID id) {
        Rapport r = trouver(id);
        exigerLecture(appelant, r);
        if (r.getDocumentId() == null) {
            throw PilotageException.introuvable("PIL_DOCUMENT_ABSENT", id);
        }
        DocumentPort.Contenu c = documents.lire(r.getDocumentId());
        return new DocumentLu(r.getDocumentNom(), r.getDocumentTypeMime(), r.getDocumentTaille() != null ? r.getDocumentTaille() : c.taille(), c.flux());
    }

    private void rediger(Rapport r, Appelant appelant, RapportSaisi s, Instant le) {
        ModeRapport mode = mode(s.mode());
        String contenu = s.contenu() == null || s.contenu().isBlank() ? null : s.contenu();
        if (contenu != null && contenu.length() > Rapport.CONTENU_MAX) {
            throw PilotageException.champ("contenu", "Contenu de " + Rapport.CONTENU_MAX + " caractères au plus");
        }
        r.rediger(mode, contenu, le);
        String ticket = s.document() == null || s.document().ticket() == null || s.document().ticket().isBlank() ? null : s.document().ticket().strip();
        if (ticket == null) {
            // Sans ticket : le document joint est conservé en mode DOCUMENT, retiré dans les autres modes.
            if (r.getDocumentId() != null && mode != ModeRapport.DOCUMENT) {
                r.retirerDocument(le);
            }
            return;
        }
        UUID documentId;
        try {
            documentId = UUID.fromString(ticket);
        } catch (IllegalArgumentException e) {
            throw PilotageException.champ("document.ticket", "Ticket de document invalide");
        }
        if (documentId.equals(r.getDocumentId())) {
            return;
        }
        // Ticket d'un dépôt fragmenté : finalité RAPPORT_ACTIVITE, référence = ce rapport, déposé par l'auteur.
        DocumentPort.Metadonnees m = documents
            .metadonnees(documentId)
            .filter(d -> DocumentPort.FINALITE.equals(d.finalite()))
            .filter(d -> appelant.id().equals(d.deposant()))
            .filter(d -> r.getId().toString().equals(d.reference()))
            .orElseThrow(() ->
                PilotageException.regle(
                    "PIL_DOCUMENT_INVALIDE",
                    "Document inconnu, ou non déposé par l'auteur pour ce rapport (finalité RAPPORT_ACTIVITE)",
                    Map.of("ticket", documentId.toString())
                )
            );
        r.joindre(documentId, m.nomFichier(), m.typeMime() == null ? "application/octet-stream" : m.typeMime(), m.taille(), le);
    }

    private Periode periode(String type, String periode) {
        TypeRapport t = ReglesActivite.valeur("type", TypeRapport.class, type, null);
        Periode p = Periode.de(t, periode == null ? null : periode.strip());
        if (p.estFuture(ReglesActivite.aujourdhui(horloge))) {
            throw PilotageException.regle("PIL_PERIODE_FUTURE", "La période n'a pas commencé", Map.of("periode", p.texte()));
        }
        return p;
    }

    private static ModeRapport mode(String mode) {
        return ReglesActivite.valeur("mode", ModeRapport.class, mode, ModeRapport.REDIGE);
    }

    private static PilotageException existant(Periode p) {
        return PilotageException.conflit(
            "PIL_RAPPORT_EXISTANT",
            "Un rapport existe déjà pour ce type et cette période",
            Map.of("type", p.type().name(), "periode", p.texte())
        );
    }

    /** Lecture : l'auteur ; une fois soumis, le destinataire et les responsables dont RAPPORT_VISER couvre l'auteur (PIL_TEAM_READ). */
    private void exigerLecture(Appelant appelant, Rapport r) {
        if (appelant.id().equals(r.getAuteurId())) {
            return;
        }
        boolean transmis = r.getEtat() != EtatRapport.BROUILLON;
        if (transmis && (appelant.id().equals(r.getDestinataireId()) || equipes.peutLire(appelant, Pouvoir.RAPPORT_VISER, r.getAuteurId()))) {
            return;
        }
        throw PilotageException.horsEquipe();
    }

    private Rapport auteur(Appelant appelant, UUID id) {
        Rapport r = trouver(id);
        if (!appelant.id().equals(r.getAuteurId())) {
            boolean lisible = r.getEtat() != EtatRapport.BROUILLON && (appelant.id().equals(r.getDestinataireId()) || equipes.peutLire(appelant, Pouvoir.RAPPORT_VISER, r.getAuteurId()));
            if (lisible) {
                throw PilotageException.reservee("Seul l'auteur modifie ou soumet son rapport");
            }
            throw PilotageException.horsEquipe();
        }
        return r;
    }

    private Rapport trouver(UUID id) {
        return rapports.findById(id).orElseThrow(() -> PilotageException.introuvable("PIL_RAPPORT_INTROUVABLE", id));
    }

    private Indicateurs indicateurs(Rapport r) {
        Indicateurs figes = r.indicateursFiges();
        return figes != null ? figes : calculer(r.getAuteurId(), r.periode());
    }

    /** Indicateurs de la période : activités faites et prévues (hors annulées et reportées), minutes, tâches terminées. */
    Indicateurs calculer(String auteur, Periode p) {
        List<Activite> l = activites.findByAuteurIdAndJourBetweenOrderByJourAscOrdreAscCreeLeAsc(auteur, p.debut(), p.fin());
        int faites = (int) l.stream().filter(a -> a.getStatut() == StatutActivite.FAITE).count();
        int prevues = (int) l.stream().filter(a -> a.getStatut() != StatutActivite.ANNULEE && a.getStatut() != StatutActivite.REPORTEE).count();
        int minutes = l.stream().filter(a -> a.getStatut() == StatutActivite.FAITE && a.getDureeMinutes() != null).mapToInt(Activite::getDureeMinutes).sum();
        int terminees = taches.termineesEntre(auteur, ReglesActivite.debutJour(p.debut()), ReglesActivite.debutJour(p.fin().plusDays(1))).size();
        return new Indicateurs(faites, prevues, minutes, terminees);
    }

    private static List<RedactionRapport.Ligne> lignes(List<Activite> l) {
        List<RedactionRapport.Ligne> r = new ArrayList<>();
        l.forEach(a -> r.add(new RedactionRapport.Ligne(a.getJour(), a.getOrdre(), a.getTitre(), a.getStatut(), a.getDureeMinutes(), a.getReporteeAu())));
        return r;
    }

    private RapportLu lu(Rapport r) {
        return new RapportLu(r, indicateurs(r), equipes.noms(comptes(r).toList()));
    }

    private static Stream<String> comptes(Rapport r) {
        return Stream.of(r.getAuteurId(), r.getDestinataireId(), r.getVisaParId());
    }
}
