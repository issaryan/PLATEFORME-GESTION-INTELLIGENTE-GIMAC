package cm.gimac.des.pilotage.application.usecase;

import cm.gimac.des.pilotage.application.port.in.Appelant;
import cm.gimac.des.pilotage.application.port.in.Page;
import cm.gimac.des.pilotage.application.port.in.TachesUseCase;
import cm.gimac.des.pilotage.application.port.out.OrganisationPort.Membre;
import cm.gimac.des.pilotage.domain.activite.ActionHistorique;
import cm.gimac.des.pilotage.domain.activite.EtatTache;
import cm.gimac.des.pilotage.domain.activite.PrioriteTache;
import cm.gimac.des.pilotage.domain.activite.Tache;
import cm.gimac.des.pilotage.domain.activite.TacheHistorique;
import cm.gimac.des.pilotage.domain.activite.TypeOrigine;
import cm.gimac.des.pilotage.domain.event.TacheAffecteePayload;
import cm.gimac.des.pilotage.domain.event.TacheCreeePayload;
import cm.gimac.des.pilotage.domain.event.TacheTermineePayload;
import cm.gimac.des.pilotage.domain.exception.PilotageException;
import cm.gimac.des.pilotage.domain.service.ReglesActivite;
import cm.gimac.des.pilotage.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.pilotage.repository.TacheHistoriqueRepository;
import cm.gimac.des.pilotage.repository.TacheRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tâches (§2.3) : responsable unique interne et actif (RG-20) ; création pour soi ou pour un membre
 * de son équipe (PIL_TASK_ASSIGN) ; modification par le créateur ou le responsable, réaffectation par
 * le créateur ; transitions du CC ; historique en ajout seul ; événements pilotage.task-* par l'outbox.
 */
@Service
@Transactional
public class TacheService implements TachesUseCase {

    static final String AGREGAT = "tache";
    private static final Pattern SERVICE = Pattern.compile("^[a-z][a-z0-9-]{1,59}$");

    private final TacheRepository taches;
    private final TacheHistoriqueRepository historique;
    private final Equipes equipes;
    private final OutboxPublisher outbox;
    private final Clock horloge;

    public TacheService(TacheRepository taches, TacheHistoriqueRepository historique, Equipes equipes, OutboxPublisher outbox, Clock horloge) {
        this.taches = taches;
        this.historique = historique;
        this.equipes = equipes;
        this.outbox = outbox;
        this.horloge = horloge;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TacheLue> taches(Appelant appelant, String vue, String etat, Integer page, Integer taille) {
        EtatTache filtre = etat == null || etat.isBlank() ? null : ReglesActivite.valeur("etat", EtatTache.class, etat, null);
        PageRequest p = PageRequest.of(ReglesActivite.page(page), ReglesActivite.taille(taille));
        org.springframework.data.domain.Page<Tache> r;
        switch (vue == null || vue.isBlank() ? "moi" : vue.strip()) {
            case "moi" -> r = taches.deResponsables(List.of(appelant.id()), filtre, p);
            case "creees" -> r = taches.confiees(appelant.id(), filtre, p);
            case "equipe" -> {
                if (!appelant.litEquipe()) {
                    throw PilotageException.horsEquipe();
                }
                List<String> ids = equipes.membres(appelant.id()).stream().map(Membre::id).toList();
                r = ids.isEmpty() ? org.springframework.data.domain.Page.empty(p) : taches.deResponsables(ids, filtre, p);
            }
            default -> throw PilotageException.champ("vue", "Vue moi, creees ou equipe attendue");
        }
        LocalDate jour = ReglesActivite.aujourdhui(horloge);
        Map<String, String> noms = equipes.noms(r.getContent().stream().flatMap(t -> Stream.of(t.getResponsableId(), t.getCreateurId())).toList());
        return new Page<>(r.getContent().stream().map(t -> new TacheLue(t, List.of(), noms, t.enRetard(jour))).toList(), r.getTotalElements(), p.getPageNumber(), p.getPageSize());
    }

    @Override
    @Transactional(readOnly = true)
    public TacheLue tache(Appelant appelant, UUID id) {
        Tache t = trouver(id);
        if (!liee(t, appelant)) {
            equipes.exigerLecture(appelant, t.getResponsableId());
        }
        return lue(t);
    }

    @Override
    public TacheLue creer(Appelant appelant, TacheSaisie saisie) {
        if (saisie == null) {
            throw PilotageException.champ("titre", "Corps de la requête absent");
        }
        String responsable = saisie.responsableId() == null || saisie.responsableId().isBlank() ? appelant.id() : saisie.responsableId().strip();
        if (!responsable.equals(appelant.id())) {
            exigerConfiable(appelant, responsable);
        }
        Instant le = horloge.instant();
        Tache t = new Tache(appelant.id(), responsable, le);
        decrire(t, saisie, le);
        taches.save(t);
        consigner(t, appelant.id(), ActionHistorique.CREEE, responsable.equals(appelant.id()) ? null : "Confiée à " + nom(responsable), le);
        publierCreation(t, appelant.id(), le);
        return lue(t);
    }

    @Override
    public TacheLue modifier(Appelant appelant, UUID id, TacheSaisie saisie) {
        Tache t = trouver(id);
        if (!liee(t, appelant)) {
            if (equipes.peutLire(appelant, t.getResponsableId())) {
                throw PilotageException.reservee("Seuls le créateur et le responsable modifient la tâche");
            }
            throw PilotageException.horsEquipe();
        }
        if (saisie == null) {
            throw PilotageException.champ("titre", "Corps de la requête absent");
        }
        Instant le = horloge.instant();
        String avant = resume(t);
        decrire(t, saisie, le);
        String apres = resume(t);
        if (!avant.equals(apres)) {
            consigner(t, appelant.id(), ActionHistorique.MODIFIEE, apres, le);
        }
        String nouveau = saisie.responsableId() == null || saisie.responsableId().isBlank() ? null : saisie.responsableId().strip();
        if (nouveau != null && !nouveau.equals(t.getResponsableId())) {
            if (!t.getCreateurId().equals(appelant.id())) {
                throw PilotageException.reservee("Seul le créateur réaffecte la tâche");
            }
            if (!nouveau.equals(appelant.id())) {
                exigerConfiable(appelant, nouveau);
            }
            String ancien = t.getResponsableId();
            t.reaffecter(nouveau, le);
            consigner(t, appelant.id(), ActionHistorique.REAFFECTEE, "De " + nom(ancien) + " à " + nom(nouveau), le);
            outbox.publish(AGREGAT, t.getId(), TacheAffecteePayload.TYPE, new TacheAffecteePayload(t.getId(), t.getEtat().name(), nouveau, ancien, appelant.id(), le, t.getTitre(), echeanceTexte(t)));
        }
        return lue(t);
    }

    @Override
    public TacheLue transiter(Appelant appelant, UUID id, String etat, String motif) {
        Tache t = trouver(id);
        if (!liee(t, appelant)) {
            if (equipes.peutLire(appelant, t.getResponsableId())) {
                throw PilotageException.reservee("Seul le responsable fait les transitions ; le créateur peut annuler");
            }
            throw PilotageException.horsEquipe();
        }
        EtatTache cible = etat == null || etat.isBlank() ? null : ReglesActivite.valeur("etat", EtatTache.class, etat, null);
        Instant le = horloge.instant();
        String m = motif == null || motif.isBlank() ? null : motif.strip();
        EtatTache avant = t.transiter(cible, m, appelant.id(), le);
        consigner(t, appelant.id(), ActionHistorique.TRANSITION, avant.name() + " vers " + cible.name() + (m == null ? "" : " : " + m), le);
        if (cible == EtatTache.TERMINEE) {
            outbox.publish(
                AGREGAT,
                t.getId(),
                TacheTermineePayload.TYPE,
                new TacheTermineePayload(
                    t.getId(),
                    t.getEtat().name(),
                    t.getResponsableId(),
                    t.getCreateurId(),
                    t.getOrigineType().name(),
                    t.getOrigineReference(),
                    appelant.id(),
                    le
                )
            );
        }
        return lue(t);
    }

    @Override
    public Creation creerPourService(String client, TacheInterne d) {
        if (d == null || d.origine() == null) {
            throw PilotageException.champ("origine", "Origine absente");
        }
        TypeOrigine type = ReglesActivite.valeur("origine.type", TypeOrigine.class, d.origine().type(), null);
        if (type == TypeOrigine.INTERNE) {
            throw PilotageException.champ("origine.type", "Une tâche d'un autre service n'a pas l'origine INTERNE");
        }
        String reference = ReglesActivite.texteObligatoire("origine.reference", d.origine().reference(), 200);
        String service = ReglesActivite.texteFacultatif("origine.service", d.origine().service(), 60);
        if (service != null && !SERVICE.matcher(service).matches()) {
            throw PilotageException.champ("origine.service", "Nom de service en minuscules attendu (par exemple homologation)");
        }
        String lien = ReglesActivite.texteFacultatif("origine.lien", d.origine().lien(), 500);
        if (lien != null && !(lien.startsWith("/") && !lien.startsWith("//"))) {
            // Lien relatif à l'interface seulement : aucune redirection vers un autre site.
            throw PilotageException.champ("origine.lien", "Chemin relatif de l'interface attendu (par exemple /des/parcours/…)");
        }
        String responsable = ReglesActivite.texteObligatoire("responsableId", d.responsableId(), 80);
        Optional<Tache> existante = taches.findFirstByOrigineReferenceAndResponsableId(reference, responsable);
        if (existante.isPresent()) {
            return new Creation(lue(existante.orElseThrow()), false);
        }
        if (!equipes.interneActif(responsable)) {
            throw PilotageException.regle("PIL_RESPONSABLE_INVALIDE", "Le responsable doit être un compte interne actif", Map.of("responsableId", responsable));
        }
        String createur = d.createurId() == null || d.createurId().isBlank() ? client : d.createurId().strip();
        Instant le = horloge.instant();
        Tache t = new Tache(createur, responsable, le);
        decrire(t, new TacheSaisie(d.titre(), d.description(), d.echeance(), d.priorite(), responsable), le);
        t.provenirDe(type, reference, service, lien);
        try {
            taches.saveAndFlush(t);
        } catch (DataIntegrityViolationException e) {
            // Demande concurrente pour la même origine et le même responsable : la première l'emporte.
            throw PilotageException.conflit("PIL_TACHE_EXISTANTE", "Tâche déjà créée pour cette origine", Map.of("reference", reference));
        }
        consigner(t, createur, ActionHistorique.CREEE, "Créée par le service " + (service == null ? client : service), le);
        publierCreation(t, createur, le);
        return new Creation(lue(t), true);
    }

    @Override
    public List<TacheLue> terminerPourService(String client, CibleInterne cible) {
        Instant le = horloge.instant();
        List<Tache> visees = visees(cible);
        for (Tache t : visees) {
            EtatTache avant = t.terminerParService(le);
            if (avant != null) {
                consigner(t, client, ActionHistorique.TRANSITION, avant.name() + " vers TERMINEE : terminée par le service " + serviceDe(t, client), le);
                outbox.publish(
                    AGREGAT,
                    t.getId(),
                    TacheTermineePayload.TYPE,
                    new TacheTermineePayload(t.getId(), t.getEtat().name(), t.getResponsableId(), t.getCreateurId(), t.getOrigineType().name(), t.getOrigineReference(), client, le)
                );
            }
        }
        return visees.stream().map(this::lue).toList();
    }

    @Override
    public List<TacheLue> rouvrirPourService(String client, CibleInterne cible) {
        Instant le = horloge.instant();
        List<Tache> visees = visees(cible);
        for (Tache t : visees) {
            EtatTache avant = t.rouvrirParService(le);
            if (avant != null) {
                consigner(t, client, ActionHistorique.TRANSITION, "TERMINEE vers EN_COURS : rouverte par le service " + serviceDe(t, client), le);
            }
        }
        return visees.stream().map(this::lue).toList();
    }

    /**
     * Tâches visées par un autre service : celles de l'origine (référence, et type s'il est donné), créées par un
     * service (jamais une tâche INTERNE), restreintes au responsable s'il est donné.
     */
    private List<Tache> visees(CibleInterne c) {
        if (c == null || c.origine() == null) {
            throw PilotageException.champ("origine", "Origine absente");
        }
        String reference = ReglesActivite.texteObligatoire("origine.reference", c.origine().reference(), 200);
        TypeOrigine type = c.origine().type() == null || c.origine().type().isBlank() ? null : ReglesActivite.valeur("origine.type", TypeOrigine.class, c.origine().type(), null);
        String responsable = c.responsableId() == null || c.responsableId().isBlank() ? null : c.responsableId().strip();
        return taches
            .findByOrigineReferenceOrderByCreeLe(reference)
            .stream()
            .filter(t -> t.getOrigineType() != TypeOrigine.INTERNE)
            .filter(t -> type == null || t.getOrigineType() == type)
            .filter(t -> responsable == null || responsable.equals(t.getResponsableId()))
            .toList();
    }

    private static String serviceDe(Tache t, String client) {
        return t.getOrigineService() == null ? client : t.getOrigineService();
    }

    private void publierCreation(Tache t, String acteur, Instant le) {
        outbox.publish(
            AGREGAT,
            t.getId(),
            TacheCreeePayload.TYPE,
            new TacheCreeePayload(
                t.getId(),
                t.getEtat().name(),
                t.getPriorite().name(),
                t.getResponsableId(),
                t.getCreateurId(),
                t.getOrigineType().name(),
                t.getOrigineReference(),
                acteur,
                le
            )
        );
        if (!t.getResponsableId().equals(t.getCreateurId())) {
            outbox.publish(AGREGAT, t.getId(), TacheAffecteePayload.TYPE, new TacheAffecteePayload(t.getId(), t.getEtat().name(), t.getResponsableId(), null, acteur, le, t.getTitre(), echeanceTexte(t)));
        }
    }

    /** Confier à un autre : PIL_TASK_ASSIGN (403 PIL_ACTION_RESERVEE), membre de l'équipe (403 PIL_HORS_EQUIPE), actif (422). */
    private void exigerConfiable(Appelant appelant, String responsable) {
        if (!appelant.confie()) {
            throw PilotageException.reservee("Confier une tâche exige la permission PIL_TASK_ASSIGN");
        }
        Membre m = equipes.membre(appelant.id(), responsable).orElseThrow(PilotageException::horsEquipe);
        if (!m.actif()) {
            throw PilotageException.regle("PIL_RESPONSABLE_INVALIDE", "Le responsable doit être un compte interne actif", Map.of("responsableId", responsable));
        }
    }

    private void decrire(Tache t, TacheSaisie s, Instant le) {
        String titre = ReglesActivite.texteObligatoire("titre", s.titre(), ReglesActivite.TITRE_MAX);
        String description = ReglesActivite.texteFacultatif("description", s.description(), ReglesActivite.DESCRIPTION_MAX);
        LocalDate echeance = ReglesActivite.lireJourFacultatif("echeance", s.echeance());
        PrioriteTache priorite = ReglesActivite.valeur("priorite", PrioriteTache.class, s.priorite(), PrioriteTache.NORMALE);
        t.decrire(titre, description, echeance, priorite, le);
    }

    private static String resume(Tache t) {
        return "Intitulé « " + t.getTitre() + " », échéance " + (t.getEcheance() == null ? "aucune" : t.getEcheance()) + ", priorité " + t.getPriorite() +
            (t.getDescription() == null ? "" : ", description de " + t.getDescription().length() + " caractères");
    }

    private void consigner(Tache t, String acteur, ActionHistorique action, String detail, Instant le) {
        String d = detail == null ? null : detail.length() > 1200 ? detail.substring(0, 1200) : detail;
        historique.save(new TacheHistorique(t.getId(), le, acteur, action, d));
    }

    private Tache trouver(UUID id) {
        return taches.findById(id).orElseThrow(() -> PilotageException.introuvable("PIL_TACHE_INTROUVABLE", id));
    }

    private static boolean liee(Tache t, Appelant appelant) {
        return appelant.id().equals(t.getResponsableId()) || appelant.id().equals(t.getCreateurId());
    }

    private String nom(String id) {
        String n = equipes.noms(List.of(id)).get(id);
        return n == null ? id : n;
    }

    private TacheLue lue(Tache t) {
        List<TacheHistorique> h = historique.findByTacheIdOrderByQuandAsc(t.getId());
        List<String> ids = new ArrayList<>(List.of(t.getResponsableId(), t.getCreateurId()));
        h.stream().map(TacheHistorique::getActeurId).filter(Objects::nonNull).forEach(ids::add);
        return new TacheLue(t, h, equipes.noms(ids), t.enRetard(ReglesActivite.aujourdhui(horloge)));
    }

    private static String echeanceTexte(Tache t) {
        return t.getEcheance() == null ? null : t.getEcheance().toString();
    }
}
