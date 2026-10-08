package cm.gimac.des.iam.application.usecase;

import cm.gimac.des.iam.application.port.out.Annuaire;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.compte.NouveauCompte;
import cm.gimac.des.iam.domain.compte.Profil;
import cm.gimac.des.iam.domain.demande.ContenuDemande;
import cm.gimac.des.iam.domain.demande.Demande;
import cm.gimac.des.iam.domain.demande.EtatDemande;
import cm.gimac.des.iam.domain.demande.TypeDemande;
import cm.gimac.des.iam.domain.event.DemandeDecideePayload;
import cm.gimac.des.iam.domain.event.DemandeSoumisePayload;
import cm.gimac.des.iam.domain.pouvoir.Portee;
import cm.gimac.des.iam.domain.pouvoir.Pouvoirs;
import cm.gimac.des.iam.domain.service.ReglesAdministration;
import cm.gimac.des.iam.domain.service.ReglesDemandes;
import cm.gimac.des.iam.domain.service.ReglesPouvoirs;
import cm.gimac.des.iam.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.iam.infrastructure.persistence.DemandeAdministration;
import cm.gimac.des.iam.infrastructure.persistence.DemandeAdministrationRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Demandes des chefs à l'administration (D-81, docs/lot1/pouvoirs-hierarchiques.md §4).
 *
 * <ul>
 *   <li>Soumission selon la matrice des pouvoirs : la cible (ou, pour une création, l'unité et le n+1
 *       proposés) est dans la portée de l'action du demandeur (403 ORG_HORS_PERIMETRE) ; les profils
 *       proposés sont dans ses profils demandables (422 ORG_PROFIL_NON_DEMANDABLE) ; une seule demande
 *       EN_ATTENTE par cible et par type (409 ORG_DEMANDE_EXISTANTE).</li>
 *   <li>Acceptation par l'administration : permission de l'acte (IAM_ACCOUNT_CREATE, IAM_ACCOUNT_ROLES,
 *       IAM_ACCOUNT_STATUS) et réauthentification FIDO2 (contrôlée par la ressource), puis exécution par
 *       les cas d'usage habituels (création avec invitation et rattachement, profils, statut), avec leurs
 *       règles (profils privilégiés, dernier super administrateur, journal).</li>
 *   <li>Refus motivé, annulation par le demandeur tant que la demande est en attente.</li>
 *   <li>Événements iam.request-submitted.v1 (à l'administration) et iam.request-decided.v1 (au
 *       demandeur) par l'outbox, dans la transaction.</li>
 * </ul>
 */
@Service
@Transactional
public class DemandesService {

    static final String AGREGAT = "request";
    static final int TAILLE_DEFAUT = 25;
    static final int TAILLE_MAX = 100;
    /** Permissions dont les titulaires reçoivent les demandes soumises (§4). */
    static final Set<String> TRAITEMENT = Set.of("IAM_ACCOUNT_CREATE", "IAM_ACCOUNT_ROLES");

    /** Page de demandes, avec son numéro et sa taille effectifs. */
    public record PageDemandes(List<Demande> contenu, long total, int page, int taille) {}

    private final DemandeAdministrationRepository depot;
    private final PouvoirsService pouvoirs;
    private final OrganisationService organisation;
    private final AdministrationComptesService comptes;
    private final Annuaire annuaire;
    private final JournalActes journal;
    private final OutboxPublisher outbox;
    private final ObjectMapper json;
    private final Clock clock;

    @SuppressWarnings("java:S107")
    public DemandesService(
        DemandeAdministrationRepository depot,
        PouvoirsService pouvoirs,
        OrganisationService organisation,
        AdministrationComptesService comptes,
        Annuaire annuaire,
        JournalActes journal,
        OutboxPublisher outbox,
        ObjectMapper json,
        Clock clock
    ) {
        this.depot = depot;
        this.pouvoirs = pouvoirs;
        this.organisation = organisation;
        this.comptes = comptes;
        this.annuaire = annuaire;
        this.journal = journal;
        this.outbox = outbox;
        this.json = json;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ soumission

    public Demande soumettre(Acteur acteur, TypeDemande type, String cibleId, ContenuDemande contenu, String motif) {
        Compte demandeur = annuaire.lire(acteur.id()).orElseThrow(() -> IamException.introuvable("IAM_ACCOUNT_NOT_FOUND", acteur.id()));
        String m = ReglesDemandes.motif(motif, "motif");
        String cible = cibleId == null || cibleId.isBlank() ? null : cibleId.trim();
        ContenuDemande c = ReglesDemandes.normaliser(type, cible, contenu);

        Pouvoirs p = pouvoirs.pouvoirs(demandeur);
        Portee portee = p.portee(ReglesDemandes.action(type));
        ReglesPouvoirs.exigerCouverture(portee != Portee.AUCUNE, ReglesDemandes.action(type));
        OrganisationService.Perimetre perimetre = organisation.perimetre(demandeur.id(), portee);
        Set<String> couverts = new HashSet<>();
        perimetre.comptes().forEach(x -> couverts.add(x.id()));

        Compte compteCible = null;
        if (type == TypeDemande.CREATION_COMPTE) {
            verifierPlaceProposee(demandeur, perimetre, couverts, c);
        } else {
            ReglesPouvoirs.exigerCouverture(couverts.contains(cible), ReglesDemandes.action(type));
            compteCible = annuaire.lire(cible).orElseThrow(() -> IamException.interdit("ORG_HORS_PERIMETRE", "Hors du périmètre"));
            verifierChangement(type, compteCible, c);
        }
        ReglesPouvoirs.exigerDemandables(ReglesDemandes.profilsProposes(c), p.profilsDemandables());
        verifierRattachementInstitution(type, compteCible, c);

        String cle = ReglesDemandes.cleCible(type, cible, c);
        if (depot.existsByTypeAndCleCibleAndEtat(type, cle, EtatDemande.EN_ATTENTE)) {
            throw IamException.conflit("ORG_DEMANDE_EXISTANTE", "Une demande de ce type est déjà en attente pour cette cible", Map.of("type", type.name()));
        }
        if (type == TypeDemande.CREATION_COMPTE && (annuaire.parIdentifiant(c.identifiant()).isPresent() || annuaire.emailPris(c.email(), null))) {
            throw IamException.conflit("IAM_ACCOUNT_CONFLICT", "Identifiant ou courriel déjà utilisé", Map.of());
        }
        DemandeAdministration d = new DemandeAdministration(
            type,
            demandeur.id(),
            demandeur.nomAffiche(),
            compteCible == null ? null : compteCible.id(),
            compteCible == null ? null : compteCible.nomAffiche(),
            cle,
            ecrire(c),
            m,
            clock.instant()
        );
        try {
            d = depot.saveAndFlush(d);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw IamException.conflit("ORG_DEMANDE_EXISTANTE", "Une demande de ce type est déjà en attente pour cette cible", Map.of("type", type.name()));
        }
        journal.demande(acteur, "DEMANDE_SOUMISE", d.getId().toString(), type.name(), m, List.of("cible=" + d.getCibleId()));
        List<String> destinataires = administrateurs(demandeur.id());
        outbox.publish(
            AGREGAT,
            d.getId(),
            DemandeSoumisePayload.TYPE,
            new DemandeSoumisePayload(d.getId().toString(), type.name(), d.getEtat().name(), demandeur.id(), d.getCibleId(), destinataires, d.getCreeeLe())
        );
        return vue(d);
    }

    /**
     * Création : le n+1 proposé est le demandeur ou un compte de son périmètre ; l'unité proposée est la
     * sienne, celle d'un compte de son périmètre ou, pour la portée UNITES, l'une de ses unités dirigées.
     * Puis les règles du rattachement (unité active, responsable interne actif).
     */
    private void verifierPlaceProposee(Compte demandeur, OrganisationService.Perimetre perimetre, Set<String> couverts, ContenuDemande c) {
        Set<String> responsables = new HashSet<>(couverts);
        responsables.add(demandeur.id());
        Set<String> unites = new HashSet<>(perimetre.unites());
        if (demandeur.unite() != null) {
            unites.add(demandeur.unite());
        }
        perimetre.comptes().stream().filter(x -> x.unite() != null).forEach(x -> unites.add(x.unite()));
        ReglesPouvoirs.exigerCouverture(responsables.contains(c.responsableId()) && unites.contains(c.unite()), ReglesDemandes.action(TypeDemande.CREATION_COMPTE));
        organisation.verifierRattachementNouveau(c.unite(), c.responsableId());
    }

    /**
     * Rattachement à une institution (règle de l'administration des comptes) vérifié dès la soumission,
     * pour ne pas accepter une demande que l'acceptation refuserait : un compte créé par demande est un
     * compte interne du GIMAC, sans institution ; des profils demandés gardent la cohérence entre
     * profil externe (attribut gimac_externe, D-94) et institution du compte visé.
     */
    private void verifierRattachementInstitution(TypeDemande type, Compte cible, ContenuDemande c) {
        switch (type) {
            case CREATION_COMPTE -> ReglesAdministration.verifierRattachement(c.profils(), null, annuaire.profilsExternes());
            case PROFILS -> ReglesAdministration.verifierRattachement(ReglesDemandes.profilsApres(cible.profils(), c), cible.institutionId(), annuaire.profilsExternes());
            case STATUT -> {
                // Le statut ne change ni les profils ni l'institution.
            }
        }
    }

    /** Une demande de profils ou de statut doit changer quelque chose (400 ORG_DEMANDE_INVALIDE sinon). */
    private static void verifierChangement(TypeDemande type, Compte cible, ContenuDemande c) {
        boolean change = type == TypeDemande.STATUT
            ? !c.actif().equals(cible.actif())
            : !ReglesDemandes.profilsApres(cible.profils(), c).equals(new java.util.TreeSet<>(cible.profils()));
        if (!change) {
            throw IamException.invalide("ORG_DEMANDE_INVALIDE", "La demande ne change rien au compte visé", Map.of("champ", "contenu"));
        }
    }

    /** Comptes actifs qui peuvent traiter une demande (IAM_ACCOUNT_CREATE ou IAM_ACCOUNT_ROLES), sauf le demandeur. */
    private List<String> administrateurs(String demandeurId) {
        Set<String> profils = new HashSet<>();
        for (Profil p : annuaire.profils()) {
            if (p.permissions().stream().anyMatch(TRAITEMENT::contains)) {
                profils.add(p.code());
            }
        }
        return annuaire.lister().stream()
            .filter(c -> c.actif() && !c.id().equals(demandeurId) && c.profils().stream().anyMatch(profils::contains))
            .map(Compte::id)
            .sorted()
            .toList();
    }

    // ------------------------------------------------------------------ lectures

    /**
     * Demandes du compte (vue « miennes », PIL_ACTIVITY_OWN) ou à traiter par l'administration (vue
     * « a-traiter », IAM_ACCOUNT_READ), filtrées par état, les plus récentes en tête.
     */
    @Transactional(readOnly = true)
    public PageDemandes lister(Acteur acteur, String vue, String etat, Integer page, Integer taille) {
        String v = vue == null || vue.isBlank() ? "miennes" : vue.trim().toLowerCase(Locale.ROOT);
        EtatDemande e = etat(etat);
        int p = page == null ? 0 : page;
        int t = taille == null ? TAILLE_DEFAUT : taille;
        if (p < 0 || t < 1 || t > TAILLE_MAX) {
            throw IamException.invalide("ORG_DEMANDE_INVALIDE", "Page à partir de 0, taille de 1 à " + TAILLE_MAX, Map.of("champ", "page"));
        }
        PageRequest pr = PageRequest.of(p, t);
        org.springframework.data.domain.Page<DemandeAdministration> r;
        switch (v) {
            case "miennes" -> {
                exiger(acteur, "PIL_ACTIVITY_OWN");
                r = e == null ? depot.findByDemandeurIdOrderByCreeeLeDesc(acteur.id(), pr) : depot.findByDemandeurIdAndEtatOrderByCreeeLeDesc(acteur.id(), e, pr);
            }
            case "a-traiter" -> {
                exiger(acteur, "IAM_ACCOUNT_READ");
                r = e == null ? depot.findAllByOrderByCreeeLeDesc(pr) : depot.findByEtatOrderByCreeeLeDesc(e, pr);
            }
            default -> throw IamException.invalide("ORG_DEMANDE_INVALIDE", "Vue attendue : miennes ou a-traiter", Map.of("champ", "vue"));
        }
        return new PageDemandes(r.getContent().stream().map(this::vue).toList(), r.getTotalElements(), p, t);
    }

    /** Une demande, pour son demandeur ou l'administration (IAM_ACCOUNT_READ) ; sinon 404. */
    @Transactional(readOnly = true)
    public Demande lire(Acteur acteur, UUID id) {
        return vue(visible(acteur, id));
    }

    // ------------------------------------------------------------------ décisions

    /** Annulation par le demandeur, tant que la demande est en attente. */
    public Demande annuler(Acteur acteur, UUID id) {
        DemandeAdministration d = visible(acteur, id);
        if (!d.getDemandeurId().equals(acteur.id())) {
            throw IamException.interdit("ORG_DEMANDE_INTERDITE", "Seul le demandeur annule sa demande");
        }
        ReglesDemandes.exigerEnAttente(d.getEtat());
        d.decider(EtatDemande.ANNULEE, acteur.id(), acteur.nom(), null, clock.instant());
        journal.demande(acteur, "DEMANDE_ANNULEE", id.toString(), d.getType().name(), null, List.of());
        return vue(depot.save(d));
    }

    /**
     * Acceptation : l'acte est exécuté par les cas d'usage habituels, avec leurs règles et leur journal ;
     * l'appelant a vérifié la réauthentification FIDO2.
     */
    public Demande accepter(Acteur acteur, UUID id, String motifDecision) {
        DemandeAdministration d = depot.findById(id).orElseThrow(() -> IamException.introuvable("ORG_DEMANDE_INTROUVABLE", id));
        exiger(acteur, ReglesDemandes.permissionActe(d.getType()));
        ReglesDemandes.exigerEnAttente(d.getEtat());
        pasSaPropreDemande(acteur, d);
        ContenuDemande c = lireContenu(d);
        String motifActe = tronquer("Demande " + d.getId() + " : " + d.getMotif(), 1000);
        switch (d.getType()) {
            case CREATION_COMPTE -> {
                organisation.verifierRattachementNouveau(c.unite(), c.responsableId());
                AdministrationComptesService.Creation creation = comptes.creer(
                    acteur,
                    new NouveauCompte(c.identifiant(), c.prenom(), c.nom(), c.email(), c.telephone(), c.fonction(), null, c.profils()),
                    true,
                    false
                );
                Compte cree = organisation.rattacher(acteur, creation.compte().id(), c.unite(), c.responsableId());
                d.definirCible(cree.id(), cree.nomAffiche());
            }
            case PROFILS -> {
                Compte cible = comptes.lire(d.getCibleId());
                comptes.definirProfils(acteur, cible.id(), ReglesDemandes.profilsApres(cible.profils(), c), motifActe);
            }
            case STATUT -> comptes.definirActif(acteur, d.getCibleId(), c.actif(), motifActe);
        }
        return decider(acteur, d, EtatDemande.ACCEPTEE, motifDecision == null || motifDecision.isBlank() ? null : ReglesDemandes.motif(motifDecision, "motif"));
    }

    /** Refus motivé par l'administration (motif de 5 à 500 caractères). */
    public Demande refuser(Acteur acteur, UUID id, String motif) {
        DemandeAdministration d = depot.findById(id).orElseThrow(() -> IamException.introuvable("ORG_DEMANDE_INTROUVABLE", id));
        String m = ReglesDemandes.motif(motif, "motif");
        ReglesDemandes.exigerEnAttente(d.getEtat());
        pasSaPropreDemande(acteur, d);
        return decider(acteur, d, EtatDemande.REFUSEE, m);
    }

    private Demande decider(Acteur acteur, DemandeAdministration d, EtatDemande etat, String motif) {
        Instant le = clock.instant();
        d.decider(etat, acteur.id(), acteur.nom(), motif, le);
        DemandeAdministration enregistree = depot.saveAndFlush(d);
        journal.demande(
            acteur,
            etat == EtatDemande.ACCEPTEE ? "DEMANDE_ACCEPTEE" : "DEMANDE_REFUSEE",
            d.getId().toString(),
            d.getType().name(),
            motif,
            List.of("demandeur=" + d.getDemandeurId(), "cible=" + d.getCibleId())
        );
        outbox.publish(
            AGREGAT,
            d.getId(),
            DemandeDecideePayload.TYPE,
            new DemandeDecideePayload(d.getId().toString(), d.getType().name(), etat.name(), d.getDemandeurId(), d.getCibleId(), acteur.id(), List.of(d.getDemandeurId()), le)
        );
        return vue(enregistree);
    }

    // ------------------------------------------------------------------ outils

    /** Un administrateur ne décide pas sa propre demande : il l'annule, ou un pair la décide. */
    private static void pasSaPropreDemande(Acteur acteur, DemandeAdministration d) {
        if (d.getDemandeurId().equals(acteur.id())) {
            throw IamException.interdit("IAM_SELF_ADMINISTRATION", "Une demande est décidée par un autre compte que son demandeur");
        }
    }

    private DemandeAdministration visible(Acteur acteur, UUID id) {
        return depot
            .findById(id)
            .filter(d -> d.getDemandeurId().equals(acteur.id()) || acteur.peut("IAM_ACCOUNT_READ"))
            .orElseThrow(() -> IamException.introuvable("ORG_DEMANDE_INTROUVABLE", id));
    }

    private static void exiger(Acteur acteur, String permission) {
        if (!acteur.peut(permission)) {
            throw IamException.interdit("ORG_DEMANDE_INTERDITE", "Permission exigée : " + permission);
        }
    }

    private static EtatDemande etat(String etat) {
        if (etat == null || etat.isBlank()) {
            return null;
        }
        try {
            return EtatDemande.valueOf(etat.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw IamException.invalide("ORG_DEMANDE_INVALIDE", "État inconnu", Map.of("champ", "etat"));
        }
    }

    private String ecrire(ContenuDemande c) {
        try {
            return json.writeValueAsString(c);
        } catch (JacksonException e) {
            throw new IllegalStateException("contenu de demande non sérialisable", e);
        }
    }

    private ContenuDemande lireContenu(DemandeAdministration d) {
        try {
            return json.readValue(d.getContenu(), ContenuDemande.class);
        } catch (JacksonException e) {
            throw new IllegalStateException("contenu de demande illisible", e);
        }
    }

    private Demande vue(DemandeAdministration d) {
        Demande.Personne cible = d.getCibleId() == null ? null : new Demande.Personne(d.getCibleId(), d.getCibleNom());
        Demande.Decision decision = d.getEtat() == EtatDemande.ACCEPTEE || d.getEtat() == EtatDemande.REFUSEE
            ? new Demande.Decision(new Demande.Personne(d.getDecisionParId(), d.getDecisionParNom()), d.getDecisionMotif(), d.getDecisionLe())
            : null;
        return new Demande(d.getId(), d.getType(), new Demande.Personne(d.getDemandeurId(), d.getDemandeurNom()), cible, lireContenu(d), d.getMotif(), d.getEtat(), decision, d.getCreeeLe());
    }

    private static String tronquer(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
