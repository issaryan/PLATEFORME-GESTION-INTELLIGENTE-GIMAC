package cm.gimac.des.iam.application.usecase.sandbox;

import cm.gimac.des.iam.application.port.out.sandbox.VerrouTaches;
import cm.gimac.des.iam.application.usecase.AdministrationComptesService;
import cm.gimac.des.iam.application.usecase.CatalogueProfils;
import cm.gimac.des.iam.domain.AccessReview;
import cm.gimac.des.iam.domain.AccessReviewDecision;
import cm.gimac.des.iam.domain.Collaborator;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.enumeration.CollaboratorStatus;
import cm.gimac.des.iam.domain.enumeration.HabilitationLevel;
import cm.gimac.des.iam.domain.enumeration.ReviewDecision;
import cm.gimac.des.iam.domain.enumeration.ReviewState;
import cm.gimac.des.iam.domain.sandbox.ReexamenAcces;
import cm.gimac.des.iam.repository.AccessReviewDecisionRepository;
import cm.gimac.des.iam.repository.AccessReviewRepository;
import cm.gimac.des.iam.repository.CollaboratorRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Campagnes trimestrielles de réexamen des comptes et de toutes leurs permissions, SBX_* comprises (IAM-18). */
@Service
public class ReexamensAccesService {

    public static final Acteur SYSTEME = new Acteur("systeme-iam", "MS-IAM, réexamen trimestriel", Set.of());
    static final String TACHE = "reexamen-acces-trimestriel";

    public record Campagne(UUID id, String period, ReviewState state, Instant openedAt, UUID approverId, Instant closedAt, String digest, long enAttente) {}
    public record Acces(
        String compteId,
        String identifiant,
        String nomAffiche,
        String institutionId,
        List<String> profils,
        List<String> permissions,
        ReviewDecision decision,
        String comment,
        UUID decidedBy,
        Instant decidedAt
    ) {}
    public record VueCampagne(Campagne campagne, List<Acces> acces) {}

    private final AccessReviewRepository reviews;
    private final AccessReviewDecisionRepository decisions;
    private final CollaboratorRepository collaborateurs;
    private final AdministrationComptesService comptes;
    private final CatalogueProfils catalogue;
    private final JournalSandbox journal;
    private final VerrouTaches verrou;
    private final Clock clock;

    public ReexamensAccesService(
        AccessReviewRepository reviews,
        AccessReviewDecisionRepository decisions,
        CollaboratorRepository collaborateurs,
        AdministrationComptesService comptes,
        CatalogueProfils catalogue,
        JournalSandbox journal,
        VerrouTaches verrou,
        Clock clock
    ) {
        this.reviews = reviews;
        this.decisions = decisions;
        this.collaborateurs = collaborateurs;
        this.comptes = comptes;
        this.catalogue = catalogue;
        this.journal = journal;
        this.verrou = verrou;
        this.clock = clock;
    }

    /** Passage trimestriel : une seule réplique et une seule campagne par période. */
    @Transactional
    public boolean ouvrirTrimestre() {
        if (!verrou.prendre(TACHE)) {
            return false;
        }
        String periode = ReexamenAcces.periode(clock);
        if (reviews.findByPeriod(periode).isPresent()) {
            return false;
        }
        ouvrir(SYSTEME, periode);
        return true;
    }

    /** Ouverture explicite de la période courante (recette et reprise), idempotente. */
    @Transactional
    public Campagne ouvrirCourante(Acteur acteur) {
        String periode = ReexamenAcces.periode(clock);
        return reviews.findByPeriod(periode).map(this::campagne).orElseGet(() -> ouvrir(acteur, periode));
    }

    private Campagne ouvrir(Acteur acteur, String periode) {
        AccessReview review = reviews.save(new AccessReview().period(periode).state(ReviewState.OUVERTE).openedAt(clock.instant()));
        Map<String, Set<String>> compositions = catalogue.compositions();
        tousComptes().forEach(c -> {
            Set<String> permissions = new TreeSet<>();
            c.profils().forEach(p -> permissions.addAll(compositions.getOrDefault(p, Set.of())));
            AccessReviewDecision d = new AccessReviewDecision()
                .review(review)
                .collaborator(collaborateur(c))
                .decision(ReviewDecision.EN_ATTENTE)
                .profilesSnapshot(ReexamenAcces.canonique(c.profils()))
                .permissionsSnapshot(ReexamenAcces.canonique(permissions));
            decisions.save(d);
        });
        journal.objet(acteur, "REEXAMEN_OUVERT", review.getId().toString(), periode, null, List.of());
        return campagne(review);
    }

    @Transactional(readOnly = true)
    public List<Campagne> lister() {
        return reviews.findAllByOrderByOpenedAtDesc().stream().map(this::campagne).toList();
    }

    @Transactional(readOnly = true)
    public VueCampagne lire(UUID id) {
        AccessReview review = review(id);
        List<Acces> acces = decisions.findByReviewIdOrderByCollaboratorFederatedId(id).stream().map(this::acces).toList();
        return new VueCampagne(campagne(review), acces);
    }

    /** Décide un accès en attente ; réduction et révocation passent par AdministrationComptesService. */
    @Transactional
    public VueCampagne decider(Acteur acteur, UUID reviewId, String compteId, ReviewDecision decision, Set<String> profils, String commentaire) {
        AccessReview review = reviewOuverte(reviewId);
        AccessReviewDecision d = decisions.findByReviewIdAndCollaboratorFederatedId(reviewId, compteId)
            .orElseThrow(() -> IamException.introuvable("IAM_REVIEW_ACCESS_NOT_FOUND", compteId));
        if (d.getDecision() != ReviewDecision.EN_ATTENTE) {
            throw IamException.conflit("IAM_REVIEW_DECISION_DONE", "Cet accès a déjà été décidé", Map.of("compteId", compteId));
        }
        if (decision == null || decision == ReviewDecision.EN_ATTENTE) {
            throw IamException.invalide("IAM_REVIEW_DECISION_REQUIRED", "Décision MAINTENU, REDUIT ou REVOQUE attendue", Map.of());
        }
        Set<String> cibles = profils == null ? Set.of() : Set.copyOf(profils);
        Compte c = comptes.lire(compteId);
        if (decision == ReviewDecision.MAINTENU) {
            if (!cibles.isEmpty()) {
                throw IamException.invalide("IAM_REVIEW_PROFILES_REFUSED", "Un maintien ne modifie pas les profils", Map.of());
            }
        } else {
            if (commentaire == null || commentaire.isBlank()) {
                throw IamException.invalide("IAM_REASON_REQUIRED", "Motif obligatoire", Map.of());
            }
            if (decision == ReviewDecision.REDUIT && (cibles.isEmpty() || c.profils().equals(cibles) || !c.profils().containsAll(cibles) || !ReexamenAcces.liste(d.getProfilesSnapshot()).containsAll(cibles))) {
                throw IamException.invalide("IAM_REVIEW_REDUCTION_INVALID", "La réduction retire au moins un profil, sans en ajouter", Map.of("profils", c.profils().stream().sorted().toList()));
            }
            comptes.definirProfils(acteur, compteId, decision == ReviewDecision.REVOQUE ? Set.of() : cibles, commentaire);
        }
        d.decision(decision).comment(commentaire).decidedBy(acteurUuid(acteur)).decidedAt(clock.instant());
        decisions.save(d);
        journal.objet(acteur, "REEXAMEN_DECISION_" + decision, compteId, c.identifiant(), commentaire, List.of(review.getPeriod()));
        return lire(reviewId);
    }

    /** Clôture seulement quand tous les accès sont décidés ; l'empreinte scelle l'instantané et les décisions. */
    @Transactional
    public Campagne cloturer(Acteur acteur, UUID id) {
        AccessReview review = reviewOuverte(id);
        List<AccessReviewDecision> lignes = decisions.findByReviewIdOrderByCollaboratorFederatedId(id);
        long enAttente = lignes.stream().filter(d -> d.getDecision() == ReviewDecision.EN_ATTENTE).count();
        if (enAttente > 0) {
            throw IamException.conflit("IAM_REVIEW_INCOMPLETE", "Tous les accès doivent être décidés avant la clôture", Map.of("enAttente", enAttente));
        }
        review.state(ReviewState.CLOSE).approverId(acteurUuid(acteur)).closedAt(clock.instant()).digest(ReexamenAcces.empreinte(review.getPeriod(), lignes));
        reviews.save(review);
        journal.objet(acteur, "REEXAMEN_CLOS", id.toString(), review.getPeriod(), null, List.of(review.getDigest()));
        return campagne(review);
    }

    private AccessReview reviewOuverte(UUID id) {
        AccessReview review = review(id);
        if (review.getState() != ReviewState.OUVERTE) {
            throw IamException.conflit("IAM_REVIEW_CLOSED", "La campagne est close", Map.of("id", id.toString()));
        }
        return review;
    }

    private AccessReview review(UUID id) {
        return reviews.findById(id).orElseThrow(() -> IamException.introuvable("IAM_REVIEW_NOT_FOUND", id.toString()));
    }

    private Collaborator collaborateur(Compte c) {
        return collaborateurs.findByFederatedId(c.id()).orElseGet(() -> collaborateurs.save(new Collaborator()
            .federatedId(c.id())
            .displayName(c.nomAffiche())
            .email(c.email())
            .status(c.actif() ? CollaboratorStatus.ACTIVE : CollaboratorStatus.SUSPENDED)
            .level(HabilitationLevel.CONSULTATION)
            .language(c.langue() == null || c.langue().isBlank() ? "fr" : c.langue())
            .failedAttempts(0)
            .lastLoginAt(null)));
    }

    private Acces acces(AccessReviewDecision d) {
        Compte c = comptes.lire(d.getCollaborator().getFederatedId());
        return new Acces(
            c.id(), c.identifiant(), c.nomAffiche(), c.institutionId(), ReexamenAcces.liste(d.getProfilesSnapshot()), ReexamenAcces.liste(d.getPermissionsSnapshot()),
            d.getDecision(), d.getComment(), d.getDecidedBy(), d.getDecidedAt()
        );
    }

    private Campagne campagne(AccessReview r) {
        return new Campagne(r.getId(), r.getPeriod(), r.getState(), r.getOpenedAt(), r.getApproverId(), r.getClosedAt(), r.getDigest(), decisions.countByReviewIdAndDecision(r.getId(), ReviewDecision.EN_ATTENTE));
    }

    private List<Compte> tousComptes() {
        List<Compte> tous = new java.util.ArrayList<>();
        int page = 0;
        AdministrationComptesService.PageComptes p;
        do {
            p = comptes.lister(new AdministrationComptesService.Filtre(null, null, null), page++, 100);
            tous.addAll(p.contenu());
        } while (tous.size() < p.total());
        return tous;
    }

    private static UUID acteurUuid(Acteur acteur) {
        try {
            return UUID.fromString(acteur.id());
        } catch (IllegalArgumentException e) {
            throw IamException.invalide("IAM_ACTOR_INVALID", "L'acteur doit être un compte de la plateforme", Map.of());
        }
    }
}
