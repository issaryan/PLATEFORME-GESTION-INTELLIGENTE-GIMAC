package cm.gimac.des.iam.application.usecase.sandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.iam.application.port.out.sandbox.VerrouTaches;
import cm.gimac.des.iam.application.usecase.AdministrationComptesService;
import cm.gimac.des.iam.application.usecase.CatalogueProfils;
import cm.gimac.des.iam.domain.AccessReview;
import cm.gimac.des.iam.domain.AccessReviewDecision;
import cm.gimac.des.iam.domain.Collaborator;
import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.Acteur;
import cm.gimac.des.iam.domain.compte.Compte;
import cm.gimac.des.iam.domain.enumeration.ReviewDecision;
import cm.gimac.des.iam.domain.enumeration.ReviewState;
import cm.gimac.des.iam.repository.AccessReviewDecisionRepository;
import cm.gimac.des.iam.repository.AccessReviewRepository;
import cm.gimac.des.iam.repository.CollaboratorRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReexamensAccesServiceTest {

    static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    static final UUID ACTEUR_ID = UUID.fromString("00000000-0000-0000-0000-000000000102");
    static final Acteur ACTEUR = new Acteur(ACTEUR_ID.toString(), "RSSI", Set.of("IAM_ACCOUNT_ROLES"));
    static final Instant T = Instant.parse("2026-10-27T12:00:00Z");

    @Mock AccessReviewRepository reviews;
    @Mock AccessReviewDecisionRepository decisions;
    @Mock CollaboratorRepository collaborateurs;
    @Mock AdministrationComptesService comptes;
    @Mock CatalogueProfils catalogue;
    @Mock JournalSandbox journal;
    @Mock VerrouTaches verrou;

    ReexamensAccesService service;
    AccessReview review;
    AccessReviewDecision decision;
    Compte compte;

    @BeforeEach
    void init() {
        Clock clock = Clock.fixed(T, ZoneOffset.UTC);
        service = new ReexamensAccesService(reviews, decisions, collaborateurs, comptes, catalogue, journal, verrou, clock);
        review = new AccessReview().id(ID).period("2026-T4").state(ReviewState.OUVERTE).openedAt(T);
        Collaborator collaborateur = new Collaborator().federatedId("c1").displayName("Awa Test");
        decision = new AccessReviewDecision().review(review).collaborator(collaborateur).decision(ReviewDecision.EN_ATTENTE)
            .profilesSnapshot("ROLE_SBX_TESTEUR\n").permissionsSnapshot("SBX_CER_EXECUTION\nSBX_CER_CONSULTATION\n");
        compte = new Compte("c1", "awa.test", "Awa", "Test", "awa@test.cm", true, true, T, null, null, "fr", "99001", true,
            Set.of("ROLE_SBX_TESTEUR", "ROLE_SBX_LECTEUR"), List.of(), true);
        lenient().when(reviews.findById(ID)).thenReturn(Optional.of(review));
        lenient().when(decisions.findByReviewIdAndCollaboratorFederatedId(ID, "c1")).thenReturn(Optional.of(decision));
        lenient().when(decisions.findByReviewIdOrderByCollaboratorFederatedId(ID)).thenReturn(List.of(decision));
        lenient().when(decisions.countByReviewIdAndDecision(ID, ReviewDecision.EN_ATTENTE)).thenReturn(1L);
        lenient().when(comptes.lire("c1")).thenReturn(compte);
        lenient().when(catalogue.compositions()).thenReturn(Map.of(
            "ROLE_SBX_TESTEUR", Set.of("SBX_CER_EXECUTION", "SBX_CER_CONSULTATION"),
            "ROLE_SBX_LECTEUR", Set.of("SBX_LEC_CONSULTATION")));
        lenient().when(collaborateurs.findByFederatedId("c1")).thenReturn(Optional.of(collaborateur));
        lenient().when(reviews.save(any())).thenAnswer(i -> {
            AccessReview r = i.getArgument(0);
            if (r.getId() == null) r.setId(ID);
            return r;
        });
        lenient().when(decisions.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(collaborateurs.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void ouvertureTrimestrielleCreeInstantaneSbxEtResteIdempotente() {
        when(verrou.prendre(ReexamensAccesService.TACHE)).thenReturn(true);
        when(reviews.findByPeriod("2026-T4")).thenReturn(Optional.empty(), Optional.of(review));
        when(comptes.lister(any(), eq(0), eq(100))).thenReturn(new AdministrationComptesService.PageComptes(List.of(compte), 1));
        assertThat(service.ouvrirTrimestre()).isTrue();
        org.mockito.ArgumentCaptor<AccessReviewDecision> captor = org.mockito.ArgumentCaptor.forClass(AccessReviewDecision.class);
        verify(decisions).save(captor.capture());
        assertThat(captor.getValue().getDecision()).isEqualTo(ReviewDecision.EN_ATTENTE);
        assertThat(captor.getValue().getPermissionsSnapshot()).contains("SBX_CER_EXECUTION").contains("SBX_LEC_CONSULTATION");
        assertThat(captor.getValue().getDecidedAt()).isNull();
        assertThat(service.ouvrirTrimestre()).isFalse();
        when(verrou.prendre(ReexamensAccesService.TACHE)).thenReturn(false);
        assertThat(service.ouvrirTrimestre()).isFalse();
    }

    @Test
    void ouvertureCouranteCreeCollaborateurEtReprendExistante() {
        when(reviews.findByPeriod("2026-T4")).thenReturn(Optional.empty(), Optional.of(review));
        when(comptes.lister(any(), eq(0), eq(100))).thenReturn(new AdministrationComptesService.PageComptes(List.of(compte), 1));
        when(collaborateurs.findByFederatedId("c1")).thenReturn(Optional.empty());
        assertThat(service.ouvrirCourante(ACTEUR).period()).isEqualTo("2026-T4");
        verify(collaborateurs).save(any(Collaborator.class));
        assertThat(service.ouvrirCourante(ACTEUR).id()).isEqualTo(ID);
        when(reviews.findAllByOrderByOpenedAtDesc()).thenReturn(List.of(review));
        assertThat(service.lister()).hasSize(1);
        assertThat(service.lire(ID).acces()).singleElement().satisfies(a -> {
            assertThat(a.permissions()).contains("SBX_CER_EXECUTION");
            assertThat(a.decision()).isEqualTo(ReviewDecision.EN_ATTENTE);
        });
    }

    @Test
    void maintienPuisClotureScelle() {
        var vue = service.decider(ACTEUR, ID, "c1", ReviewDecision.MAINTENU, null, null);
        assertThat(vue.acces().getFirst().decision()).isEqualTo(ReviewDecision.MAINTENU);
        verify(comptes, never()).definirProfils(any(), any(), any(), any());
        assertThatThrownBy(() -> service.decider(ACTEUR, ID, "c1", ReviewDecision.REVOQUE, null, "Départ"))
            .isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo("IAM_REVIEW_DECISION_DONE"));
        var close = service.cloturer(ACTEUR, ID);
        assertThat(close.state()).isEqualTo(ReviewState.CLOSE);
        assertThat(close.digest()).matches("^[0-9a-f]{64}$");
        assertThat(close.approverId()).isEqualTo(ACTEUR_ID);
        assertThatThrownBy(() -> service.cloturer(ACTEUR, ID)).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo("IAM_REVIEW_CLOSED"));
    }

    @Test
    void reductionEtRevocationPassentParAdministration() {
        service.decider(ACTEUR, ID, "c1", ReviewDecision.REDUIT, Set.of("ROLE_SBX_TESTEUR"), "Changement de fonction");
        verify(comptes).definirProfils(ACTEUR, "c1", Set.of("ROLE_SBX_TESTEUR"), "Changement de fonction");
        decision.decision(ReviewDecision.EN_ATTENTE);
        service.decider(ACTEUR, ID, "c1", ReviewDecision.REVOQUE, null, "Départ");
        verify(comptes).definirProfils(ACTEUR, "c1", Set.of(), "Départ");
    }

    @Test
    void decisionsInvalidesEtAccesIntrouvable() {
        assertThatThrownBy(() -> service.decider(ACTEUR, ID, "c1", ReviewDecision.EN_ATTENTE, null, null)).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo("IAM_REVIEW_DECISION_REQUIRED"));
        assertThatThrownBy(() -> service.decider(ACTEUR, ID, "c1", null, null, null)).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo("IAM_REVIEW_DECISION_REQUIRED"));
        assertThatThrownBy(() -> service.decider(ACTEUR, ID, "c1", ReviewDecision.MAINTENU, Set.of("ROLE_SBX_TESTEUR"), null)).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo("IAM_REVIEW_PROFILES_REFUSED"));
        assertThatThrownBy(() -> service.decider(ACTEUR, ID, "c1", ReviewDecision.REDUIT, Set.of("ROLE_SBX_TESTEUR"), null)).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo("IAM_REASON_REQUIRED"));
        assertThatThrownBy(() -> service.decider(ACTEUR, ID, "c1", ReviewDecision.REDUIT, Set.of("ROLE_SBX_TESTEUR"), " ")).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo("IAM_REASON_REQUIRED"));
        assertThatThrownBy(() -> service.decider(ACTEUR, ID, "c1", ReviewDecision.REDUIT, Set.of(), "x")).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo("IAM_REVIEW_REDUCTION_INVALID"));
        assertThatThrownBy(() -> service.decider(ACTEUR, ID, "c1", ReviewDecision.REDUIT, compte.profils(), "x")).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo("IAM_REVIEW_REDUCTION_INVALID"));
        assertThatThrownBy(() -> service.decider(ACTEUR, ID, "c1", ReviewDecision.REDUIT, Set.of("ROLE_INCONNU"), "x")).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo("IAM_REVIEW_REDUCTION_INVALID"));
        assertThatThrownBy(() -> service.decider(ACTEUR, ID, "c1", ReviewDecision.REDUIT, Set.of("ROLE_SBX_LECTEUR"), "x")).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo("IAM_REVIEW_REDUCTION_INVALID"));
        assertThatThrownBy(() -> service.decider(ACTEUR, ID, "inconnu", ReviewDecision.MAINTENU, null, null)).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo("IAM_REVIEW_ACCESS_NOT_FOUND"));
        verify(comptes, never()).definirProfils(any(), any(), any(), any());
    }

    @Test
    void clotureIncompleteEtLectureIntrouvable() {
        assertThatThrownBy(() -> service.cloturer(ACTEUR, ID)).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo("IAM_REVIEW_INCOMPLETE"));
        UUID inconnu = UUID.fromString("00000000-0000-0000-0000-000000000999");
        assertThatThrownBy(() -> service.lire(inconnu)).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo("IAM_REVIEW_NOT_FOUND"));
    }

    @Test
    void acteurDoitEtreUnCompte() {
        Acteur systeme = new Acteur("systeme-iam", "Système", Set.of());
        assertThatThrownBy(() -> service.decider(systeme, ID, "c1", ReviewDecision.MAINTENU, null, null)).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.code()).isEqualTo("IAM_ACTOR_INVALID"));
    }

    @Test
    void ouverturePagineeEtCompteInactifSansLangue() {
        Compte inactif = new Compte("c2", "bob.inactif", "Bob", "Inactif", null, false, false, T, null, null, null, null, true, Set.of(), List.of(), false);
        when(reviews.findByPeriod("2026-T4")).thenReturn(Optional.empty());
        when(comptes.lister(any(), eq(0), eq(100))).thenReturn(new AdministrationComptesService.PageComptes(java.util.Collections.nCopies(100, compte), 101));
        when(comptes.lister(any(), eq(1), eq(100))).thenReturn(new AdministrationComptesService.PageComptes(List.of(inactif), 101));
        when(collaborateurs.findByFederatedId(any())).thenReturn(Optional.empty());
        service.ouvrirCourante(ACTEUR);
        verify(decisions, org.mockito.Mockito.times(101)).save(any());
        org.mockito.ArgumentCaptor<Collaborator> captor = org.mockito.ArgumentCaptor.forClass(Collaborator.class);
        verify(collaborateurs, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        assertThat(captor.getValue().getStatus().name()).isEqualTo("SUSPENDED");
        assertThat(captor.getValue().getLanguage()).isEqualTo("fr");
    }

    @Test
    void langueBlancheDuCollaborateurRemplaceeParFrancais() {
        Compte blanc = new Compte("c2", "bob.inactif", "Bob", "Inactif", null, false, true, T, null, null, " ", null, true, Set.of(), List.of(), false);
        when(reviews.findByPeriod("2026-T4")).thenReturn(Optional.empty());
        when(comptes.lister(any(), eq(0), eq(100))).thenReturn(new AdministrationComptesService.PageComptes(List.of(blanc), 1));
        when(collaborateurs.findByFederatedId("c2")).thenReturn(Optional.empty());
        service.ouvrirCourante(ACTEUR);
        org.mockito.ArgumentCaptor<Collaborator> captor = org.mockito.ArgumentCaptor.forClass(Collaborator.class);
        verify(collaborateurs).save(captor.capture());
        assertThat(captor.getValue().getLanguage()).isEqualTo("fr");
    }
}
