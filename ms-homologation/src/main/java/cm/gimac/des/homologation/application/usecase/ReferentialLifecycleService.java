package cm.gimac.des.homologation.application.usecase;

import cm.gimac.des.homologation.application.port.in.ManageReferentialUseCase;
import cm.gimac.des.homologation.application.port.out.CollaboratorDirectoryPort;
import cm.gimac.des.homologation.application.port.out.PilotageTachesPort;
import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.ReferenceOperation;
import cm.gimac.des.homologation.domain.ReferenceOperationData;
import cm.gimac.des.homologation.domain.ReferentialVersion;
import cm.gimac.des.homologation.domain.enumeration.EtatVersionReferentiel;
import cm.gimac.des.homologation.domain.enumeration.TypeProjet;
import cm.gimac.des.homologation.domain.event.CatalogPublishedPayload;
import cm.gimac.des.homologation.domain.parcours.Position;
import cm.gimac.des.homologation.domain.service.ReferentialCompletenessChecker;
import cm.gimac.des.homologation.domain.service.ReferentialGraphValidator;
import cm.gimac.des.homologation.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.homologation.repository.ReferenceOperationRepository;
import cm.gimac.des.homologation.repository.ReferentialVersionRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Publication d'une version du référentiel (HOM-01 A4) : verrou, complétude, contrôle du graphe et du modèle
 * (HOM-02, D-77 §2), retrait de la version publiée précédente, numérotation, événement par l'outbox dans la même
 * transaction. Modélisateur (D-77 §4) : création, retrait, placement des opérations, arcs, pilote du brouillon ;
 * la tâche du pilote (MS-PILOTAGE, §5) naît avec le brouillon et se termine à la publication. Aucune écriture
 * n'est réservée au pilote ni au créateur (D-89) : tout porteur de HOM_REFERENTIEL_EDIT modifie un brouillon.
 */
@Service
@Transactional
public class ReferentialLifecycleService implements ManageReferentialUseCase {

    /**
     * Profils qui portent HOM_REFERENTIEL_EDIT (D-89, §1) : pilotes possibles d'un brouillon. Le pilote est le
     * responsable affiché et notifié du brouillon ; il ne le verrouille pas : tout éditeur le modifie.
     */
    public static final String PROFILS_EDITION =
        "ROLE_INGENIEUR_HOMOLOGATION,ROLE_CHEF_DES,ROLE_CHEF_DDRM,ROLE_RESPONSABLE_SERVICE,ROLE_SUPER_ADMIN";

    static final String AGREGAT = "catalog";
    static final String ORIGINE = "REFERENTIEL";

    private final ReferentialVersionRepository versions;
    private final ReferentialCompletenessChecker completeness;
    private final ReferentialGraphValidator graphValidator;
    private final OutboxPublisher outboxPublisher;
    private final Clock clock;
    private final ReferenceOperationRepository operations;
    private final CollaboratorDirectoryPort annuaire;
    private final DemandesTaches taches;
    private final Set<String> profilsPilote;

    @Autowired
    public ReferentialLifecycleService(
        ReferentialVersionRepository versions,
        ReferentialCompletenessChecker completeness,
        ReferentialGraphValidator graphValidator,
        OutboxPublisher outboxPublisher,
        Clock clock,
        ReferenceOperationRepository operations,
        CollaboratorDirectoryPort annuaire,
        DemandesTaches taches,
        @Value("${gimac.homologation.referentiel.profils-pilote:" + PROFILS_EDITION + "}") List<String> profilsPilote
    ) {
        this.versions = versions;
        this.completeness = completeness;
        this.graphValidator = graphValidator;
        this.outboxPublisher = outboxPublisher;
        this.clock = clock;
        this.operations = operations;
        this.annuaire = annuaire;
        this.taches = taches;
        this.profilsPilote = Set.copyOf(profilsPilote);
    }

    /** Publication seule (tests du cycle de vie) : ni annuaire, ni tâches. */
    public ReferentialLifecycleService(
        ReferentialVersionRepository versions,
        ReferentialCompletenessChecker completeness,
        ReferentialGraphValidator graphValidator,
        OutboxPublisher outboxPublisher,
        Clock clock
    ) {
        this(versions, completeness, graphValidator, outboxPublisher, clock, null, null, null, List.of("ROLE_INGENIEUR_HOMOLOGATION"));
    }

    @Override
    public ReferentialVersion creerBrouillon(TypeProjet type, String libelle, UUID versionSourceId) {
        return creerBrouillon(type, libelle, versionSourceId, null, null);
    }

    @Override
    public ReferentialVersion creerBrouillon(TypeProjet type, String libelle, UUID versionSourceId, UUID createurId, UUID piloteId) {
        Instant now = clock.instant();
        ReferentialVersion brouillon = versionSourceId == null
            ? ReferentialVersion.nouveauBrouillon(type, libelle, now)
            : ReferentialVersion.deriverDe(charger(versionSourceId), libelle, now);
        UUID pilote = piloteId == null ? createurId : piloteId;
        if (pilote != null) {
            verifierPilote(pilote, createurId);
            brouillon.designerPilote(pilote);
        }
        brouillon = versions.save(brouillon);
        if (pilote != null) {
            demanderTache(brouillon, pilote, createurId);
        }
        return brouillon;
    }

    @Override
    public ReferenceOperation enregistrerOperation(UUID versionId, String code, ReferenceOperationData data) {
        return verrouiller(versionId).upsertOperation(code, data);
    }

    @Override
    public ReferenceOperation creerOperation(UUID versionId, String code, ReferenceOperationData data) {
        return verrouiller(versionId).creerOperation(code, data);
    }

    @Override
    public void supprimerOperation(UUID versionId, String code) {
        ReferenceOperation op = verrouiller(versionId).retirerOperation(code);
        operations.delete(op);
    }

    @Override
    public void placer(UUID versionId, Map<String, Position> positions) {
        verrouiller(versionId).placer(positions);
    }

    @Override
    public ReferenceOperation arc(UUID versionId, String de, String vers, boolean present) {
        ReferentialVersion v = verrouiller(versionId);
        if (present && atteint(v, vers, de)) {
            throw HomologationException.conflict(
                "HOM_REFERENTIAL_CYCLE",
                "Cet arc fermerait un cycle : " + de + " succède déjà à " + vers + ".",
                Map.of("de", de, "vers", vers)
            );
        }
        return v.arc(de, vers, present);
    }

    /** {@code b} est-il atteint depuis {@code a} en suivant les successeurs (a précède b) ? */
    private static boolean atteint(ReferentialVersion v, String a, String b) {
        Deque<String> pile = new ArrayDeque<>(List.of(b));
        Set<String> vus = new HashSet<>();
        while (!pile.isEmpty()) {
            String x = pile.pop();
            if (x.equals(a)) {
                return true;
            }
            if (vus.add(x)) {
                v.operation(x).ifPresent(o -> pile.addAll(o.predecesseursListe()));
            }
        }
        return false;
    }

    @Override
    public ReferentialVersion designerPilote(UUID versionId, UUID compteId, UUID demandeurId) {
        ReferentialVersion v = verrouiller(versionId);
        v.assertModifiable();
        verifierPilote(compteId, demandeurId);
        UUID ancien = v.getPiloteId();
        if (compteId.equals(ancien)) {
            return v;
        }
        v.designerPilote(compteId);
        if (taches != null) {
            if (ancien != null) {
                taches.cloturer(new PilotageTachesPort.Designation(origine(v), ancien.toString()));
            }
            demanderTache(v, compteId, demandeurId);
        }
        return v;
    }

    /** Le pilote porte HOM_REFERENTIEL_EDIT : le demandeur lui-même, ou un compte actif d'un profil d'édition. */
    private void verifierPilote(UUID compteId, UUID demandeurId) {
        if (annuaire == null || compteId.equals(demandeurId)) {
            return;
        }
        boolean eligible = annuaire.findCollaborator(compteId).filter(c -> c.actif() && c.profils().stream().anyMatch(profilsPilote::contains)).isPresent();
        if (!eligible) {
            throw HomologationException.unprocessable("HOM_PILOTE_NOT_ELIGIBLE", "Le pilote doit être un compte actif autorisé à éditer le référentiel.");
        }
    }

    private void demanderTache(ReferentialVersion v, UUID pilote, UUID createurId) {
        if (taches == null) {
            return;
        }
        int numero = versions.maxNumero(v.getTypeProjet()).orElse(0) + 1;
        taches.creer(
            new PilotageTachesPort.Tache(
                "Compléter et publier le référentiel " + libelle(v.getTypeProjet()) + " v" + numero,
                "Brouillon « " + v.getLibelle() + " » : compléter les opérations dans le modélisateur, contrôler puis publier la version.",
                null,
                "NORMALE",
                pilote.toString(),
                createurId == null ? null : createurId.toString(),
                origine(v)
            )
        );
    }

    private static PilotageTachesPort.Origine origine(ReferentialVersion v) {
        return new PilotageTachesPort.Origine(ORIGINE, "referentiel/" + v.getId(), DemandesTaches.SERVICE, "/des/referentiels/operations?version=" + v.getId());
    }

    static String libelle(TypeProjet t) {
        return switch (t) {
            case CARTE_INTERBANCAIRE -> "Carte interbancaire";
            case PAIEMENT_MOBILE -> "Paiement mobile";
            case DELEGATAIRE -> "Délégataire";
        };
    }

    @Override
    public ReferentialVersion publier(UUID versionId) {
        ReferentialVersion v = verrouiller(versionId);
        v.assertModifiable();
        var manquants = completeness.check(v.getOperations());
        if (!manquants.isEmpty()) {
            throw HomologationException.conflict("HOM_REFERENTIAL_INCOMPLETE", "Publication refusée : opérations incomplètes", Map.of("operations", manquants));
        }
        List<String> ordre = graphValidator.validate(v.noeuds()); // lève HOM_REFERENTIAL_GRAPH_INVALID
        versions.findFirstByTypeProjetAndEtat(v.getTypeProjet(), EtatVersionReferentiel.PUBLIEE).ifPresent(ReferentialVersion::retirer);
        int numero = versions.maxNumero(v.getTypeProjet()).orElse(0) + 1;
        v.publier(numero, clock.instant(), ordre);
        outboxPublisher.publish(
            AGREGAT,
            v.getId().toString(),
            CatalogPublishedPayload.TYPE,
            new CatalogPublishedPayload(v.getId(), v.getTypeProjet(), numero, v.getDatePublication(), v.getOperations().size(), v.getVersionSourceId())
        );
        if (taches != null && v.getPiloteId() != null) {
            taches.cloturer(new PilotageTachesPort.Designation(origine(v), v.getPiloteId().toString()));
        }
        return v;
    }

    private ReferentialVersion verrouiller(UUID versionId) {
        return versions.findByIdForUpdate(versionId).orElseThrow(() -> HomologationException.notFound("HOM_REFERENTIAL_NOT_FOUND", versionId));
    }

    private ReferentialVersion charger(UUID id) {
        return versions.findById(id).orElseThrow(() -> HomologationException.notFound("HOM_REFERENTIAL_NOT_FOUND", id));
    }
}
