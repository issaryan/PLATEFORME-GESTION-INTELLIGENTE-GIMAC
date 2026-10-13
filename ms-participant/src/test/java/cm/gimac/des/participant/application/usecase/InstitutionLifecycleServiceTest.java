package cm.gimac.des.participant.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.gimac.des.participant.application.port.in.ManageInstitutionUseCase.Acteur;
import cm.gimac.des.participant.application.port.in.ManageInstitutionUseCase.Candidature;
import cm.gimac.des.participant.application.port.in.ManageInstitutionUseCase.ConditionsAdhesion;
import cm.gimac.des.participant.application.port.in.ManageInstitutionUseCase.Depot;
import cm.gimac.des.participant.application.port.out.DocumentRegistry;
import cm.gimac.des.participant.application.port.out.DocumentRegistry.Verification;
import cm.gimac.des.participant.application.port.out.StrongReauthVerifier;
import cm.gimac.des.participant.application.AdhesionProperties;
import cm.gimac.des.participant.application.AdhesionProperties.PieceDefinition;
import cm.gimac.des.participant.domain.AdhesionPiece;
import cm.gimac.des.participant.domain.Institution;
import cm.gimac.des.participant.domain.Interaction;
import cm.gimac.des.participant.domain.ParticipantException;
import cm.gimac.des.participant.domain.StatusTransition;
import cm.gimac.des.participant.domain.enumeration.AdhesionPieceState;
import cm.gimac.des.participant.domain.enumeration.ComplianceState;
import cm.gimac.des.participant.domain.enumeration.InstitutionCategory;
import cm.gimac.des.participant.domain.enumeration.InstitutionStatus;
import cm.gimac.des.participant.domain.enumeration.InteractionNature;
import cm.gimac.des.participant.domain.enumeration.MembershipMode;
import cm.gimac.des.participant.domain.enumeration.TransitionState;
import cm.gimac.des.participant.domain.event.AdhesionPieceRecordedPayload;
import cm.gimac.des.participant.domain.event.InstitutionRegisteredPayload;
import cm.gimac.des.participant.domain.event.InstitutionUpdatedPayload;
import cm.gimac.des.participant.domain.event.StatusChangedPayload;
import cm.gimac.des.participant.domain.event.TransitionApprovedPayload;
import cm.gimac.des.participant.domain.event.TransitionRejectedPayload;
import cm.gimac.des.participant.domain.event.TransitionRequestedPayload;
import cm.gimac.des.participant.infrastructure.messaging.outbox.OutboxPublisher;
import cm.gimac.des.participant.repository.AdhesionPieceRepository;
import cm.gimac.des.participant.repository.InstitutionRepository;
import cm.gimac.des.participant.repository.InteractionRepository;
import cm.gimac.des.participant.repository.StatusTransitionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Étapes 1 et 2 du parcours et automate en maker-checker (PAR-01 à PAR-04, D-57), sans base. */
@ExtendWith(MockitoExtension.class)
class InstitutionLifecycleServiceTest {

    static final Instant MAINTENANT = Instant.parse("2026-10-05T08:00:00Z");
    static final Acteur CHARGE = new Acteur(UUID.randomUUID(), "Chargé DDRM");
    static final Acteur CHEF = new Acteur(UUID.randomUUID(), "Chef DDRM");
    static final Acteur DG = new Acteur(UUID.randomUUID(), "Direction générale");
    static final String SHA = "a".repeat(64);

    @Mock
    InstitutionRepository institutions;

    @Mock
    AdhesionPieceRepository pieces;

    @Mock
    StatusTransitionRepository transitions;

    @Mock
    InteractionRepository interactions;

    @Mock
    DocumentRegistry documents;

    @Mock
    StrongReauthVerifier reauth;

    @Mock
    OutboxPublisher outbox;

    InstitutionLifecycleService service;
    Institution banque;
    List<AdhesionPiece> dossier;

    @BeforeEach
    void init() {
        AdhesionProperties props = new AdhesionProperties(
            List.of(new PieceDefinition("STATUTS", "Statuts", true), new PieceDefinition("ORGANIGRAMME", "Organigramme", false))
        );
        service = new InstitutionLifecycleService(
            institutions,
            pieces,
            transitions,
            interactions,
            documents,
            reauth,
            outbox,
            props,
            new DossierSnapshots(pieces),
            Clock.fixed(MAINTENANT, ZoneOffset.UTC)
        );
        banque = new Institution()
            .name("Banque B")
            .country("CM")
            .category(InstitutionCategory.BANQUE)
            .status(InstitutionStatus.CANDIDAT)
            .complianceState(ComplianceState.A_REGULARISER)
            .openLinkCount(0)
            .openSuspenseCount(0)
            .createdAt(MAINTENANT);
        banque.setId(UUID.randomUUID());
        dossier = new ArrayList<>();
        dossier.add(piece("STATUTS", true, AdhesionPieceState.ATTENDUE));
        dossier.add(piece("ORGANIGRAMME", false, AdhesionPieceState.ATTENDUE));
        lenient().when(institutions.findByIdForUpdate(banque.getId())).thenReturn(Optional.of(banque));
        lenient().when(institutions.findById(banque.getId())).thenReturn(Optional.of(banque));
        lenient().when(pieces.findByInstitutionIdOrderByCode(banque.getId())).thenReturn(dossier);
        lenient()
            .when(pieces.findByInstitutionIdAndCode(eq(banque.getId()), anyString()))
            .thenAnswer(inv -> dossier.stream().filter(p -> p.getCode().equals(inv.getArgument(1))).findFirst());
        lenient().when(transitions.save(any(StatusTransition.class))).thenAnswer(inv -> avecId(inv.getArgument(0)));
    }

    private static StatusTransition avecId(StatusTransition t) {
        t.setId(UUID.randomUUID());
        return t;
    }

    private AdhesionPiece piece(String code, boolean obligatoire, AdhesionPieceState etat) {
        AdhesionPiece p = new AdhesionPiece().code(code).label(code).mandatory(obligatoire).state(etat).institution(banque);
        p.setId(UUID.randomUUID());
        return p;
    }

    private static Candidature candidature(String bic) {
        return new Candidature("Banque B", "BB", "CM", InstitutionCategory.BANQUE, bic, "B001", null, "Courrier", "Carte interbancaire", "Groupe B");
    }

    private void rendreAdmissible() {
        dossier.get(0).setState(AdhesionPieceState.VALIDEE);
        banque.setParticipantCode("BQ-B-001");
        banque.setMembershipMode(MembershipMode.DIRECT);
        banque.setSettlementAccountRef("BEAC-001");
    }

    private StatusTransition proposition(InstitutionStatus de, InstitutionStatus vers, int requises, Acteur demandeur) {
        StatusTransition t = new StatusTransition()
            .fromStatus(de)
            .toStatus(vers)
            .reason("motif")
            .requesterId(demandeur.id())
            .requestedAt(MAINTENANT)
            .automaticTransition(false)
            .requiredApprovals(requises)
            .state(TransitionState.PROPOSEE)
            .institution(banque);
        t.setId(UUID.randomUUID());
        when(transitions.findById(t.getId())).thenReturn(Optional.of(t));
        return t;
    }

    @Nested
    @DisplayName("Étape 1 : prise de contact")
    class PriseDeContact {

        @Test
        @DisplayName("PAR-01, critère 1 : le candidat est enregistré avec son dossier de pièces et l'événement est publié")
        void enregistrement() {
            when(institutions.save(any(Institution.class))).thenAnswer(inv -> {
                Institution i = inv.getArgument(0);
                i.setId(UUID.randomUUID());
                return i;
            });

            Institution i = service.enregistrerCandidat(candidature("BQBBCMCX"), CHARGE);

            assertThat(i.getStatus()).isEqualTo(InstitutionStatus.CANDIDAT);
            assertThat(i.getComplianceState()).isEqualTo(ComplianceState.A_REGULARISER);
            assertThat(i.getDdrmOfficerId()).isEqualTo(CHARGE.id());
            assertThat(i.getDdrmOfficerName()).isEqualTo("Chargé DDRM");
            assertThat(i.getContactedOn()).isEqualTo(LocalDate.of(2026, 10, 5));
            assertThat(i.getCreatedAt()).isEqualTo(MAINTENANT);
            // D-104 : la filiale porte son groupe.
            assertThat(i.getGroupName()).isEqualTo("Groupe B");
            ArgumentCaptor<AdhesionPiece> p = ArgumentCaptor.forClass(AdhesionPiece.class);
            verify(pieces, org.mockito.Mockito.times(2)).save(p.capture());
            assertThat(p.getAllValues()).extracting(AdhesionPiece::getCode, AdhesionPiece::getState, AdhesionPiece::getMandatory).containsExactly(
                org.assertj.core.groups.Tuple.tuple("STATUTS", AdhesionPieceState.ATTENDUE, true),
                org.assertj.core.groups.Tuple.tuple("ORGANIGRAMME", AdhesionPieceState.ATTENDUE, false)
            );
            verify(interactions).save(any(Interaction.class));
            verify(outbox).publish(eq("institution"), eq(i.getId()), eq(InstitutionRegisteredPayload.TYPE), any(InstitutionRegisteredPayload.class));
        }

        @Test
        @DisplayName("La date de prise de contact fournie est conservée")
        void dateFournie() {
            when(institutions.save(any(Institution.class))).thenAnswer(inv -> avecIdInstitution(inv.getArgument(0)));
            Candidature c = new Candidature("A", null, "GA", InstitutionCategory.FINTECH, null, null, LocalDate.of(2026, 9, 1), null, null, null);
            assertThat(service.enregistrerCandidat(c, CHARGE).getContactedOn()).isEqualTo(LocalDate.of(2026, 9, 1));
        }

        @Test
        @DisplayName("PAR-01, critère 3 : un code d'identification déjà attribué est refusé")
        void codeDejaAttribue() {
            when(institutions.existsByIdentificationCodeAndIdNot(eq("BQBBCMCX"), any())).thenReturn(true);
            assertThatThrownBy(() -> service.enregistrerCandidat(candidature("BQBBCMCX"), CHARGE))
                .isInstanceOfSatisfying(ParticipantException.class, ex -> {
                    assertThat(ex.code()).isEqualTo("PAR_CODE_ALREADY_USED");
                    assertThat(ex.details()).containsEntry("champ", "codeIdentification");
                });
            verify(institutions, never()).save(any());
        }

        @Test
        @DisplayName("Un code banque BEAC déjà attribué est refusé")
        void codeBeacDejaAttribue() {
            when(institutions.existsByBeacBankCodeAndIdNot(eq("B001"), any())).thenReturn(true);
            assertThatThrownBy(() -> service.enregistrerCandidat(candidature(null), CHARGE))
                .isInstanceOfSatisfying(ParticipantException.class, ex -> assertThat(ex.details()).containsEntry("champ", "codeBanqueBeac"));
        }

        @Test
        @DisplayName("La fiche est modifiable tant que l'institution n'est pas close")
        void modification() {
            Institution i = service.modifierFiche(banque.getId(), candidature(null), CHEF);
            assertThat(i.getAcronym()).isEqualTo("BB");
            assertThat(i.getContactOrigin()).isEqualTo("Courrier");
            verify(outbox).publish(eq("institution"), eq(banque.getId()), eq(InstitutionUpdatedPayload.TYPE), any(InstitutionUpdatedPayload.class));
        }

        @Test
        void modificationAvecDate() {
            Candidature c = new Candidature("A", null, "GA", InstitutionCategory.FINTECH, null, null, LocalDate.of(2026, 9, 1), null, null, null);
            assertThat(service.modifierFiche(banque.getId(), c, CHEF).getContactedOn()).isEqualTo(LocalDate.of(2026, 9, 1));
        }

        @Test
        @DisplayName("Une institution close sans suite est en lecture seule")
        void modificationRefuseeSiClose() {
            banque.setStatus(InstitutionStatus.CLOS_SANS_SUITE);
            assertThatThrownBy(() -> service.modifierFiche(banque.getId(), candidature(null), CHEF))
                .isInstanceOfSatisfying(ParticipantException.class, ex -> assertThat(ex.code()).isEqualTo("PAR_INSTITUTION_CLOSED"));
        }

        @Test
        @DisplayName("Institution inconnue : 404")
        void inconnue() {
            UUID id = UUID.randomUUID();
            assertThatThrownBy(() -> service.modifierFiche(id, candidature(null), CHEF))
                .isInstanceOfSatisfying(ParticipantException.class, ex -> {
                    assertThat(ex.status()).isEqualTo(404);
                    assertThat(ex.code()).isEqualTo("PAR_INSTITUTION_NOT_FOUND");
                });
        }
    }

    private static Institution avecIdInstitution(Institution i) {
        i.setId(UUID.randomUUID());
        return i;
    }

    @Nested
    @DisplayName("Étape 2 : dossier et conditions d'adhésion")
    class Adhesion {

        @Test
        @DisplayName("Adhésion directe : compte de règlement retenu, compte de cantonnement ignoré")
        void conditionsDirectes() {
            Institution reglement = avecIdInstitution(new Institution());
            when(institutions.findById(reglement.getId())).thenReturn(Optional.of(reglement));
            Institution i = service.definirConditionsAdhesion(
                banque.getId(),
                new ConditionsAdhesion("BQ-B-001", MembershipMode.DIRECT, reglement.getId(), UUID.randomUUID(), "BEAC-001", "CANT"),
                CHARGE
            );
            assertThat(i.getParticipantCode()).isEqualTo("BQ-B-001");
            assertThat(i.getSettlementAccountRef()).isEqualTo("BEAC-001");
            assertThat(i.getRingFencedAccountRef()).isNull();
            assertThat(i.getSettlementBank()).isSameAs(reglement);
            assertThat(i.getSponsorBank()).isNull();
            verify(outbox).publish(eq("institution"), eq(banque.getId()), eq(InstitutionUpdatedPayload.TYPE), any());
        }

        @Test
        @DisplayName("Adhésion indirecte : banque marraine et compte de cantonnement retenus")
        void conditionsIndirectes() {
            Institution marraine = avecIdInstitution(new Institution());
            when(institutions.findById(marraine.getId())).thenReturn(Optional.of(marraine));
            Institution i = service.definirConditionsAdhesion(
                banque.getId(),
                new ConditionsAdhesion("EP-A-017", MembershipMode.INDIRECT, null, marraine.getId(), "X", "CANT-9"),
                CHARGE
            );
            assertThat(i.getSponsorBank()).isSameAs(marraine);
            assertThat(i.getRingFencedAccountRef()).isEqualTo("CANT-9");
            assertThat(i.getSettlementAccountRef()).isNull();
            assertThat(i.getSettlementBank()).isNull();
        }

        @Test
        @DisplayName("DDR-03 : conditions vides, code invalide ou comptes manquants (RG-11, RG-12) refusés champ par champ")
        void conditionsIncompletes() {
            assertThatThrownBy(() -> service.definirConditionsAdhesion(banque.getId(), new ConditionsAdhesion(null, null, null, null, null, null), CHARGE))
                .isInstanceOfSatisfying(ParticipantException.class, ex -> {
                    assertThat(ex.code()).isEqualTo("PAR_CONDITIONS_INVALID");
                    assertThat(ex.status()).isEqualTo(422);
                    assertThat(((Map<?, ?>) ex.details().get("champs")).keySet()).map(String::valueOf).containsExactlyInAnyOrder("codeParticipant", "mode");
                });
            assertThatThrownBy(() ->
                service.definirConditionsAdhesion(banque.getId(), new ConditionsAdhesion("BAL@01 !", MembershipMode.INDIRECT, null, null, null, null), CHARGE)
            ).isInstanceOfSatisfying(ParticipantException.class, ex ->
                assertThat(((Map<?, ?>) ex.details().get("champs")).keySet()).map(String::valueOf).containsExactlyInAnyOrder("codeParticipant", "banqueMarraineId", "compteCantonnement")
            );
            assertThatThrownBy(() ->
                service.definirConditionsAdhesion(banque.getId(), new ConditionsAdhesion("BQ-BAL-074", MembershipMode.DIRECT, null, null, " ", null), CHARGE)
            ).isInstanceOfSatisfying(ParticipantException.class, ex -> assertThat(((Map<?, ?>) ex.details().get("champs")).keySet()).map(String::valueOf).containsExactlyInAnyOrder("compteReglement"));
            assertThat(banque.getParticipantCode()).isNull();
            verifyNoInteractions(outbox);
        }

        @Test
        @DisplayName("Une institution ne peut être sa propre marraine")
        void propreMarraine() {
            assertThatThrownBy(() ->
                service.definirConditionsAdhesion(
                    banque.getId(),
                    new ConditionsAdhesion("EP-A-017", MembershipMode.INDIRECT, null, banque.getId(), null, "C"),
                    CHARGE
                )
            ).isInstanceOfSatisfying(ParticipantException.class, ex -> assertThat(ex.code()).isEqualTo("PAR_SPONSOR_INVALID"));
        }

        @Test
        @DisplayName("Code participant déjà attribué : refus")
        void codeParticipantPris() {
            when(institutions.existsByParticipantCodeAndIdNot("BQ-B-001", banque.getId())).thenReturn(true);
            assertThatThrownBy(() ->
                service.definirConditionsAdhesion(banque.getId(), new ConditionsAdhesion("BQ-B-001", MembershipMode.DIRECT, null, null, "R", null), CHARGE)
            ).isInstanceOfSatisfying(ParticipantException.class, ex -> assertThat(ex.details()).containsEntry("champ", "codeParticipant"));
        }

        @Test
        @DisplayName("Conditions d'adhésion figées après le prononcé")
        void conditionsApresAdhesion() {
            banque.setStatus(InstitutionStatus.ADHERENT);
            assertThatThrownBy(() ->
                service.definirConditionsAdhesion(banque.getId(), new ConditionsAdhesion("C", MembershipMode.DIRECT, null, null, "R", null), CHARGE)
            ).isInstanceOfSatisfying(ParticipantException.class, ex -> assertThat(ex.code()).isEqualTo("PAR_INSTITUTION_STATUS"));
        }

        @Test
        @DisplayName("Dépôt d'une pièce : référence et empreinte vérifiées puis rattachées")
        void depot() {
            UUID doc = UUID.randomUUID();
            when(documents.verifier(doc, SHA)).thenReturn(Verification.CONFORME);
            AdhesionPiece p = service.deposerPiece(banque.getId(), "STATUTS", new Depot(doc, SHA, "statuts.pdf"), CHARGE);
            assertThat(p.getState()).isEqualTo(AdhesionPieceState.RECUE);
            assertThat(p.getDocumentId()).isEqualTo(doc);
            assertThat(p.getDocumentSha256()).isEqualTo(SHA);
            assertThat(p.getFileName()).isEqualTo("statuts.pdf");
            assertThat(p.getDepositedById()).isEqualTo(CHARGE.id());
            assertThat(p.getDepositedAt()).isEqualTo(MAINTENANT);
            verify(outbox).publish(eq("adhesion"), eq(banque.getId()), eq(AdhesionPieceRecordedPayload.TYPE), any(AdhesionPieceRecordedPayload.class));
        }

        @Test
        @DisplayName("Une pièce rejetée peut être redéposée ; son examen précédent est effacé")
        void redepot() {
            AdhesionPiece p = dossier.get(0);
            p.setState(AdhesionPieceState.REJETEE);
            p.setRejectionReason("illisible");
            p.setReviewedById(CHEF.id());
            when(documents.verifier(any(), any())).thenReturn(Verification.CONFORME);
            service.deposerPiece(banque.getId(), "STATUTS", new Depot(UUID.randomUUID(), SHA, null), CHARGE);
            assertThat(p.getState()).isEqualTo(AdhesionPieceState.RECUE);
            assertThat(p.getRejectionReason()).isNull();
            assertThat(p.getReviewedById()).isNull();
        }

        @Test
        @DisplayName("Pièce inconnue du registre documentaire : refus")
        void depotInconnu() {
            when(documents.verifier(any(), any())).thenReturn(Verification.INCONNUE);
            assertThatThrownBy(() -> service.deposerPiece(banque.getId(), "STATUTS", new Depot(UUID.randomUUID(), SHA, null), CHARGE))
                .isInstanceOfSatisfying(ParticipantException.class, ex -> assertThat(ex.code()).isEqualTo("PAR_DOCUMENT_UNKNOWN"));
        }

        @Test
        @DisplayName("Empreinte divergente : refus, rien n'est rattaché")
        void depotDivergent() {
            when(documents.verifier(any(), any())).thenReturn(Verification.DIVERGENTE);
            assertThatThrownBy(() -> service.deposerPiece(banque.getId(), "STATUTS", new Depot(UUID.randomUUID(), SHA, null), CHARGE))
                .isInstanceOfSatisfying(ParticipantException.class, ex -> assertThat(ex.code()).isEqualTo("PAR_DOCUMENT_HASH_MISMATCH"));
            assertThat(dossier.get(0).getState()).isEqualTo(AdhesionPieceState.ATTENDUE);
            verify(outbox, never()).publish(anyString(), any(UUID.class), anyString(), any());
        }

        @Test
        @DisplayName("Une pièce validée ne se redépose pas")
        void depotSurValidee() {
            dossier.get(0).setState(AdhesionPieceState.VALIDEE);
            assertThatThrownBy(() -> service.deposerPiece(banque.getId(), "STATUTS", new Depot(UUID.randomUUID(), SHA, null), CHARGE))
                .isInstanceOfSatisfying(ParticipantException.class, ex -> assertThat(ex.code()).isEqualTo("PAR_PIECE_ALREADY_VALIDATED"));
        }

        @Test
        @DisplayName("Pièce hors du dossier : 404")
        void pieceInconnue() {
            assertThatThrownBy(() -> service.validerPiece(banque.getId(), "INCONNUE", CHEF))
                .isInstanceOfSatisfying(ParticipantException.class, ex -> assertThat(ex.code()).isEqualTo("PAR_PIECE_NOT_FOUND"));
        }

        @Test
        @DisplayName("Validation d'une pièce reçue")
        void validation() {
            dossier.get(0).setState(AdhesionPieceState.RECUE);
            AdhesionPiece p = service.validerPiece(banque.getId(), "STATUTS", CHEF);
            assertThat(p.getState()).isEqualTo(AdhesionPieceState.VALIDEE);
            assertThat(p.getReviewedById()).isEqualTo(CHEF.id());
            assertThat(p.getReviewedAt()).isEqualTo(MAINTENANT);
        }

        @Test
        @DisplayName("Rejet motivé d'une pièce reçue")
        void rejet() {
            dossier.get(0).setState(AdhesionPieceState.RECUE);
            AdhesionPiece p = service.rejeterPiece(banque.getId(), "STATUTS", "Pièce illisible", CHEF);
            assertThat(p.getState()).isEqualTo(AdhesionPieceState.REJETEE);
            assertThat(p.getRejectionReason()).isEqualTo("Pièce illisible");
        }

        @Test
        @DisplayName("Rejet sans motif refusé")
        void rejetSansMotif() {
            assertThatThrownBy(() -> service.rejeterPiece(banque.getId(), "STATUTS", " ", CHEF))
                .isInstanceOfSatisfying(ParticipantException.class, ex -> assertThat(ex.code()).isEqualTo("PAR_REASON_REQUIRED"));
        }

        @Test
        @DisplayName("Seule une pièce reçue s'examine")
        void examenPieceNonRecue() {
            assertThatThrownBy(() -> service.validerPiece(banque.getId(), "STATUTS", CHEF))
                .isInstanceOfSatisfying(ParticipantException.class, ex -> assertThat(ex.code()).isEqualTo("PAR_PIECE_NOT_RECEIVED"));
        }
    }

    @Nested
    @DisplayName("Automate et maker-checker (6.25)")
    class Transitions {

        @Test
        @DisplayName("Prérequis de l'adhésion lus sans verrou")
        void prerequis() {
            assertThat(service.prerequis(banque.getId(), InstitutionStatus.ADHERENT)).extracting("code").contains("PIECE_STATUTS", "CODE_PARTICIPANT");
        }

        @Test
        void prerequisInstitutionInconnue() {
            assertThatThrownBy(() -> service.prerequis(UUID.randomUUID(), InstitutionStatus.ADHERENT)).isInstanceOf(ParticipantException.class);
        }

        @Test
        @DisplayName("Proposition d'adhésion d'un dossier complet : PROPOSEE, une approbation requise, événement publié")
        void propositionAdhesion() {
            rendreAdmissible();
            StatusTransition t = service.proposerTransition(banque.getId(), InstitutionStatus.ADHERENT, "Dossier complet", null, CHARGE);
            assertThat(t.getState()).isEqualTo(TransitionState.PROPOSEE);
            assertThat(t.getFromStatus()).isEqualTo(InstitutionStatus.CANDIDAT);
            assertThat(t.getRequiredApprovals()).isEqualTo(1);
            assertThat(t.getRequesterId()).isEqualTo(CHARGE.id());
            assertThat(banque.getStatus()).isEqualTo(InstitutionStatus.CANDIDAT);
            ArgumentCaptor<Object> evt = ArgumentCaptor.forClass(Object.class);
            verify(outbox).publish(eq("transition"), eq(banque.getId()), eq(TransitionRequestedPayload.TYPE), evt.capture());
            // D-98 : nom de l'institution et écran où l'approbateur prononce la transition (tâche de MS-PILOTAGE).
            TransitionRequestedPayload p = (TransitionRequestedPayload) evt.getValue();
            assertThat(p.institutionNom()).isEqualTo(banque.getName());
            assertThat(p.lien()).isEqualTo("/des/institutions/" + banque.getId() + "?onglet=transitions");
            assertThat(p.approbationsRequises()).isEqualTo(1);
            assertThat(p.vers()).isEqualTo("ADHERENT");
        }

        @Test
        @DisplayName("PAR-03, critère 3 : proposition refusée avec la liste des prérequis manquants")
        void propositionSansPrerequis() {
            assertThatThrownBy(() -> service.proposerTransition(banque.getId(), InstitutionStatus.ADHERENT, "motif", null, CHARGE))
                .isInstanceOfSatisfying(ParticipantException.class, ex -> {
                    assertThat(ex.code()).isEqualTo("PAR_PREREQUISITES_MISSING");
                    assertThat(ex.details()).containsKey("manquants");
                });
            verify(transitions, never()).save(any());
        }

        @Test
        @DisplayName("Une seule proposition en attente par institution")
        void propositionDejaEnAttente() {
            rendreAdmissible();
            when(transitions.existsByInstitutionIdAndState(banque.getId(), TransitionState.PROPOSEE)).thenReturn(true);
            assertThatThrownBy(() -> service.proposerTransition(banque.getId(), InstitutionStatus.ADHERENT, "motif", null, CHARGE))
                .isInstanceOfSatisfying(ParticipantException.class, ex -> assertThat(ex.code()).isEqualTo("PAR_TRANSITION_PENDING"));
        }

        @Test
        @DisplayName("Motif obligatoire")
        void propositionSansMotif() {
            assertThatThrownBy(() -> service.proposerTransition(banque.getId(), InstitutionStatus.ADHERENT, null, null, CHARGE))
                .isInstanceOfSatisfying(ParticipantException.class, ex -> assertThat(ex.code()).isEqualTo("PAR_REASON_REQUIRED"));
        }

        @Test
        @DisplayName("PAR-02, critère 3 : transition hors automate refusée")
        void propositionHorsAutomate() {
            assertThatThrownBy(() -> service.proposerTransition(banque.getId(), InstitutionStatus.ACTIF, "motif", null, CHARGE))
                .isInstanceOfSatisfying(ParticipantException.class, ex -> assertThat(ex.code()).isEqualTo("PAR_TRANSITION_NOT_ALLOWED"));
        }

        @Test
        @DisplayName("Prononcé de l'adhésion : ADHERENT, date, auteur, conformité, journal et événement lu par l'homologation")
        void approbationAdhesion() {
            rendreAdmissible();
            StatusTransition t = proposition(InstitutionStatus.CANDIDAT, InstitutionStatus.ADHERENT, 1, CHARGE);
            when(reauth.verifier("assertion", CHEF.id(), "STATUS_APPROVE")).thenReturn(true);

            service.approuverTransition(banque.getId(), t.getId(), CHEF, "assertion");

            assertThat(t.getState()).isEqualTo(TransitionState.APPROUVEE);
            assertThat(t.getApproverId()).isEqualTo(CHEF.id());
            assertThat(t.getDecidedAt()).isEqualTo(MAINTENANT);
            assertThat(banque.getStatus()).isEqualTo(InstitutionStatus.ADHERENT);
            assertThat(banque.getAdmittedById()).isEqualTo(CHEF.id());
            assertThat(banque.getAdmittedAt()).isEqualTo(MAINTENANT);
            assertThat(banque.getJoinedOn()).isEqualTo(LocalDate.of(2026, 10, 5));
            assertThat(banque.getComplianceState()).isEqualTo(ComplianceState.CONFORME);
            ArgumentCaptor<Interaction> journal = ArgumentCaptor.forClass(Interaction.class);
            verify(interactions).save(journal.capture());
            assertThat(journal.getValue().getNature()).isEqualTo(InteractionNature.CHANGEMENT_STATUT);
            ArgumentCaptor<Object> evt = ArgumentCaptor.forClass(Object.class);
            verify(outbox).publish(eq("institution"), eq(banque.getId()), eq(StatusChangedPayload.TYPE), evt.capture());
            StatusChangedPayload payload = (StatusChangedPayload) evt.getValue();
            assertThat(payload.statut()).isEqualTo("ADHERENT");
            assertThat(payload.ancienStatut()).isEqualTo("CANDIDAT");
            assertThat(payload.institutionId()).isEqualTo(banque.getId());
            // D-98 : approbation définitive publiée sur le sujet des transitions (clôture de la tâche de l'approbateur).
            ArgumentCaptor<Object> approbation = ArgumentCaptor.forClass(Object.class);
            verify(outbox).publish(eq("transition"), eq(banque.getId()), eq(TransitionApprovedPayload.TYPE), approbation.capture());
            assertThat((TransitionApprovedPayload) approbation.getValue()).isEqualTo(
                new TransitionApprovedPayload(banque.getId(), t.getId(), "CANDIDAT", "ADHERENT", CHEF.id(), 1, 1, true, CHARGE.id(), "Banque B", "/des/institutions/" + banque.getId() + "?onglet=transitions")
            );
        }

        @Test
        @DisplayName("Clôture sans suite : motif et date consignés")
        void approbationClotureSansSuite() {
            StatusTransition t = proposition(InstitutionStatus.CANDIDAT, InstitutionStatus.CLOS_SANS_SUITE, 1, CHARGE);
            when(reauth.verifier(any(), any(), any())).thenReturn(true);
            service.approuverTransition(banque.getId(), t.getId(), CHEF, "a");
            assertThat(banque.getStatus()).isEqualTo(InstitutionStatus.CLOS_SANS_SUITE);
            assertThat(banque.getClosureReason()).isEqualTo("motif");
            assertThat(banque.getClosedAt()).isEqualTo(MAINTENANT);
        }

        @Test
        @DisplayName("PAR-02, critère 4 : l'auteur de la proposition ne peut l'approuver")
        void approbateurIdentique() {
            StatusTransition t = proposition(InstitutionStatus.CANDIDAT, InstitutionStatus.CLOS_SANS_SUITE, 1, CHARGE);
            assertThatThrownBy(() -> service.approuverTransition(banque.getId(), t.getId(), CHARGE, "a"))
                .isInstanceOfSatisfying(ParticipantException.class, ex -> {
                    assertThat(ex.code()).isEqualTo("PAR_SEPARATION_OF_DUTIES");
                    assertThat(ex.status()).isEqualTo(403);
                });
            assertThat(t.getState()).isEqualTo(TransitionState.PROPOSEE);
        }

        @Test
        @DisplayName("IAM-07 : sans assertion de réauthentification, 428")
        void sansReauth() {
            StatusTransition t = proposition(InstitutionStatus.CANDIDAT, InstitutionStatus.CLOS_SANS_SUITE, 1, CHARGE);
            assertThatThrownBy(() -> service.approuverTransition(banque.getId(), t.getId(), CHEF, null))
                .isInstanceOfSatisfying(ParticipantException.class, ex -> assertThat(ex.status()).isEqualTo(428));
            assertThatThrownBy(() -> service.approuverTransition(banque.getId(), t.getId(), CHEF, " "))
                .isInstanceOfSatisfying(ParticipantException.class, ex -> assertThat(ex.status()).isEqualTo(428));
        }

        @Test
        @DisplayName("IAM-07 : assertion refusée par MS-IAM, 403")
        void reauthInvalide() {
            StatusTransition t = proposition(InstitutionStatus.CANDIDAT, InstitutionStatus.CLOS_SANS_SUITE, 1, CHARGE);
            when(reauth.verifier(any(), any(), any())).thenReturn(false);
            assertThatThrownBy(() -> service.approuverTransition(banque.getId(), t.getId(), CHEF, "a"))
                .isInstanceOfSatisfying(ParticipantException.class, ex -> assertThat(ex.code()).isEqualTo("PAR_REAUTH_INVALID"));
        }

        @Test
        @DisplayName("Statut modifié depuis la proposition : refus")
        void propositionPerimee() {
            StatusTransition t = proposition(InstitutionStatus.CANDIDAT, InstitutionStatus.CLOS_SANS_SUITE, 1, CHARGE);
            banque.setStatus(InstitutionStatus.ADHERENT);
            when(reauth.verifier(any(), any(), any())).thenReturn(true);
            assertThatThrownBy(() -> service.approuverTransition(banque.getId(), t.getId(), CHEF, "a"))
                .isInstanceOfSatisfying(ParticipantException.class, ex -> assertThat(ex.code()).isEqualTo("PAR_TRANSITION_STALE"));
        }

        @Test
        @DisplayName("Prérequis réévalués à l'approbation")
        void prerequisReevalues() {
            StatusTransition t = proposition(InstitutionStatus.CANDIDAT, InstitutionStatus.ADHERENT, 1, CHARGE);
            when(reauth.verifier(any(), any(), any())).thenReturn(true);
            assertThatThrownBy(() -> service.approuverTransition(banque.getId(), t.getId(), CHEF, "a"))
                .isInstanceOfSatisfying(ParticipantException.class, ex -> assertThat(ex.code()).isEqualTo("PAR_PREREQUISITES_MISSING"));
            assertThat(banque.getStatus()).isEqualTo(InstitutionStatus.CANDIDAT);
        }

        @Test
        @DisplayName("RG-14 : levée de suspension à deux approbateurs distincts")
        void leveeDeSuspension() {
            banque.setStatus(InstitutionStatus.SUSPENDU);
            banque.setAttestationIssuedAt(MAINTENANT);
            banque.setGoLiveAuthorizedAt(MAINTENANT);
            banque.setComplianceState(ComplianceState.CONFORME);
            StatusTransition t = proposition(InstitutionStatus.SUSPENDU, InstitutionStatus.ACTIF, 2, CHARGE);
            when(reauth.verifier(any(), any(), any())).thenReturn(true);

            service.approuverTransition(banque.getId(), t.getId(), CHEF, "a");
            assertThat(t.getState()).isEqualTo(TransitionState.PROPOSEE);
            assertThat(t.getApproverId()).isEqualTo(CHEF.id());
            assertThat(banque.getStatus()).isEqualTo(InstitutionStatus.SUSPENDU);
            // D-98 : première approbation publiée, non définitive (la tâche repasse à prendre pour un second approbateur).
            verify(outbox).publish(
                "transition",
                banque.getId(),
                TransitionApprovedPayload.TYPE,
                new TransitionApprovedPayload(banque.getId(), t.getId(), "SUSPENDU", "ACTIF", CHEF.id(), 2, 1, false, CHARGE.id(), "Banque B", "/des/institutions/" + banque.getId() + "?onglet=transitions")
            );

            assertThatThrownBy(() -> service.approuverTransition(banque.getId(), t.getId(), CHEF, "a"))
                .isInstanceOfSatisfying(ParticipantException.class, ex -> assertThat(ex.code()).isEqualTo("PAR_SEPARATION_OF_DUTIES"));

            service.approuverTransition(banque.getId(), t.getId(), DG, "a");
            assertThat(t.getState()).isEqualTo(TransitionState.APPROUVEE);
            assertThat(t.getSecondApproverId()).isEqualTo(DG.id());
            assertThat(t.getSecondApprovedAt()).isEqualTo(MAINTENANT);
            assertThat(banque.getStatus()).isEqualTo(InstitutionStatus.ACTIF);
            verify(outbox).publish(
                "transition",
                banque.getId(),
                TransitionApprovedPayload.TYPE,
                new TransitionApprovedPayload(banque.getId(), t.getId(), "SUSPENDU", "ACTIF", DG.id(), 2, 2, true, CHARGE.id(), "Banque B", "/des/institutions/" + banque.getId() + "?onglet=transitions")
            );
        }

        @Test
        @DisplayName("Proposition déjà tranchée : refus")
        void dejaTranchee() {
            StatusTransition t = proposition(InstitutionStatus.CANDIDAT, InstitutionStatus.CLOS_SANS_SUITE, 1, CHARGE);
            t.setState(TransitionState.REJETEE);
            assertThatThrownBy(() -> service.approuverTransition(banque.getId(), t.getId(), CHEF, "a"))
                .isInstanceOfSatisfying(ParticipantException.class, ex -> assertThat(ex.code()).isEqualTo("PAR_TRANSITION_NOT_PENDING"));
        }

        @Test
        @DisplayName("Proposition d'une autre institution : 404")
        void autreInstitution() {
            StatusTransition t = proposition(InstitutionStatus.CANDIDAT, InstitutionStatus.CLOS_SANS_SUITE, 1, CHARGE);
            t.setInstitution(avecIdInstitution(new Institution()));
            assertThatThrownBy(() -> service.approuverTransition(banque.getId(), t.getId(), CHEF, "a"))
                .isInstanceOfSatisfying(ParticipantException.class, ex -> assertThat(ex.code()).isEqualTo("PAR_TRANSITION_NOT_FOUND"));
        }

        @Test
        @DisplayName("PAR-02, critère 2 : rejet motivé, statut inchangé, événement publié")
        void rejet() {
            StatusTransition t = proposition(InstitutionStatus.CANDIDAT, InstitutionStatus.CLOS_SANS_SUITE, 1, CHARGE);
            service.rejeterTransition(banque.getId(), t.getId(), "Candidat relancé", CHEF);
            assertThat(t.getState()).isEqualTo(TransitionState.REJETEE);
            assertThat(t.getRejectionReason()).isEqualTo("Candidat relancé");
            assertThat(t.getApproverId()).isEqualTo(CHEF.id());
            assertThat(banque.getStatus()).isEqualTo(InstitutionStatus.CANDIDAT);
            // DDR-08 : le refus porte le proposant, pour que MS-NOTIFICATION l'en avise.
            verify(outbox).publish(
                "transition",
                banque.getId(),
                TransitionRejectedPayload.TYPE,
                new TransitionRejectedPayload(
                    banque.getId(),
                    t.getId(),
                    "CANDIDAT",
                    "CLOS_SANS_SUITE",
                    "Candidat relancé",
                    CHEF.id(),
                    CHARGE.id(),
                    "Banque B",
                    "/des/institutions/" + banque.getId() + "?onglet=transitions"
                )
            );
        }

        @Test
        @DisplayName("Rejet par le second approbateur d'une levée de suspension")
        void rejetSecondApprobateur() {
            StatusTransition t = proposition(InstitutionStatus.SUSPENDU, InstitutionStatus.ACTIF, 2, CHARGE);
            t.setApproverId(CHEF.id());
            service.rejeterTransition(banque.getId(), t.getId(), "Non", DG);
            assertThat(t.getSecondApproverId()).isEqualTo(DG.id());
        }

        @Test
        @DisplayName("Le demandeur ne peut statuer sur sa propre proposition")
        void rejetParDemandeur() {
            StatusTransition t = proposition(InstitutionStatus.CANDIDAT, InstitutionStatus.CLOS_SANS_SUITE, 1, CHARGE);
            assertThatThrownBy(() -> service.rejeterTransition(banque.getId(), t.getId(), "motif", CHARGE))
                .isInstanceOfSatisfying(ParticipantException.class, ex -> assertThat(ex.code()).isEqualTo("PAR_SEPARATION_OF_DUTIES"));
        }

        @Test
        @DisplayName("Suspension : la pièce de décision est conservée avec la proposition")
        void suspensionAvecPiece() {
            banque.setStatus(InstitutionStatus.ACTIF);
            UUID piece = UUID.randomUUID();
            StatusTransition t = service.proposerTransition(banque.getId(), InstitutionStatus.SUSPENDU, "Décision du conseil", piece, CHARGE);
            assertThat(t.getEvidenceDocumentId()).isEqualTo(piece);
            assertThat(Map.of("n", t.getRequiredApprovals())).containsEntry("n", 1);
        }

        @Test
        @DisplayName("Transition sans donnée propre (A_REGULARISER) : seul le statut change")
        void transitionSimple() {
            banque.setStatus(InstitutionStatus.ACTIF);
            StatusTransition t = proposition(InstitutionStatus.ACTIF, InstitutionStatus.A_REGULARISER, 1, CHARGE);
            when(reauth.verifier(any(), any(), any())).thenReturn(true);
            service.approuverTransition(banque.getId(), t.getId(), CHEF, "a");
            assertThat(banque.getStatus()).isEqualTo(InstitutionStatus.A_REGULARISER);
            assertThat(banque.getAdmittedAt()).isNull();
            assertThat(banque.getClosedAt()).isNull();
        }

        @Test
        @DisplayName("Le résumé du journal est tronqué à 500 caractères")
        void resumeTronque() {
            StatusTransition t = proposition(InstitutionStatus.CANDIDAT, InstitutionStatus.CLOS_SANS_SUITE, 1, CHARGE);
            t.setReason("x".repeat(900));
            when(reauth.verifier(any(), any(), any())).thenReturn(true);
            service.approuverTransition(banque.getId(), t.getId(), CHEF, "a");
            ArgumentCaptor<Interaction> journal = ArgumentCaptor.forClass(Interaction.class);
            verify(interactions).save(journal.capture());
            assertThat(journal.getValue().getSummary()).hasSize(500);
        }
    }

    @Nested
    @DisplayName("Étape 3 : ouverture d'une liaison par MS-HOMOLOGATION (D-57)")
    class OuvertureDeLiaison {

        final UUID liaison = UUID.randomUUID();

        @Test
        @DisplayName("Un adhérent passe automatiquement à EN_HOMOLOGATION, transition tracée et événement publié")
        void adherentEnHomologation() {
            banque.setStatus(InstitutionStatus.ADHERENT);
            StatusTransition t = service.debuterHomologation(banque.getId(), liaison, "PAIEMENT_MOBILE").orElseThrow();
            assertThat(banque.getStatus()).isEqualTo(InstitutionStatus.EN_HOMOLOGATION);
            assertThat(t.getAutomaticTransition()).isTrue();
            assertThat(t.getState()).isEqualTo(TransitionState.APPROUVEE);
            assertThat(t.getFromStatus()).isEqualTo(InstitutionStatus.ADHERENT);
            assertThat(t.getReason()).contains(liaison.toString(), "PAIEMENT_MOBILE");
            assertThat(t.getRequesterId()).isEqualTo(InstitutionLifecycleService.SYSTEME.id());
            verify(interactions).save(any());
            verify(outbox).publish(eq("institution"), eq(banque.getId()), eq(StatusChangedPayload.TYPE), any(StatusChangedPayload.class));
        }

        @Test
        @DisplayName("Seconde liaison (rang 35) : l'institution déjà en homologation reste en l'état")
        void dejaEnHomologation() {
            banque.setStatus(InstitutionStatus.EN_HOMOLOGATION);
            assertThat(service.debuterHomologation(banque.getId(), liaison, "CARTE_INTERBANCAIRE")).isEmpty();
            verify(transitions, never()).save(any());
            verifyNoInteractions(outbox);
        }

        @Test
        @DisplayName("Institution non adhérente : signalé, jamais appliqué")
        void nonAdherente() {
            assertThat(service.debuterHomologation(banque.getId(), liaison, "PAIEMENT_MOBILE")).isEmpty();
            assertThat(banque.getStatus()).isEqualTo(InstitutionStatus.CANDIDAT);
            verifyNoInteractions(outbox);
        }

        @Test
        void institutionInconnue() {
            assertThatThrownBy(() -> service.debuterHomologation(UUID.randomUUID(), liaison, "PAIEMENT_MOBILE")).isInstanceOf(ParticipantException.class);
        }
    }

    @Nested
    @DisplayName("Jalon EN_PRODUCTION d'une liaison (D-93 §4)")
    class MiseEnProduction {

        final UUID liaison = UUID.randomUUID();

        @Test
        @DisplayName("Une institution en homologation passe automatiquement à ACTIF, transition tracée, journalisée et publiée")
        void enHomologationActive() {
            banque.setStatus(InstitutionStatus.EN_HOMOLOGATION);
            StatusTransition t = service.mettreEnProduction(banque.getId(), liaison, "LIA-2026-001", "CARTE_INTERBANCAIRE").orElseThrow();
            assertThat(banque.getStatus()).isEqualTo(InstitutionStatus.ACTIF);
            assertThat(t.getAutomaticTransition()).isTrue();
            assertThat(t.getState()).isEqualTo(TransitionState.APPROUVEE);
            assertThat(t.getFromStatus()).isEqualTo(InstitutionStatus.EN_HOMOLOGATION);
            assertThat(t.getToStatus()).isEqualTo(InstitutionStatus.ACTIF);
            assertThat(t.getReason()).contains("EN_PRODUCTION", "LIA-2026-001", "CARTE_INTERBANCAIRE");
            assertThat(t.getRequesterId()).isEqualTo(InstitutionLifecycleService.SYSTEME.id());
            ArgumentCaptor<Interaction> journal = ArgumentCaptor.forClass(Interaction.class);
            verify(interactions).save(journal.capture());
            assertThat(journal.getValue().getNature()).isEqualTo(InteractionNature.CHANGEMENT_STATUT);
            ArgumentCaptor<StatusChangedPayload> charge = ArgumentCaptor.forClass(StatusChangedPayload.class);
            verify(outbox).publish(eq("institution"), eq(banque.getId()), eq(StatusChangedPayload.TYPE), charge.capture());
            assertThat(charge.getValue().ancienStatut()).isEqualTo("EN_HOMOLOGATION");
            assertThat(charge.getValue().statut()).isEqualTo("ACTIF");
        }

        @Test
        @DisplayName("Sans référence, le motif cite l'identifiant de la liaison")
        void sansReference() {
            banque.setStatus(InstitutionStatus.EN_HOMOLOGATION);
            StatusTransition t = service.mettreEnProduction(banque.getId(), liaison, null, null).orElseThrow();
            assertThat(t.getReason()).contains(liaison.toString()).doesNotContain("(");
        }

        @Test
        @DisplayName("Idempotent : une institution déjà active (seconde liaison) reste en l'état, sans trace ni événement")
        void dejaActive() {
            banque.setStatus(InstitutionStatus.ACTIF);
            assertThat(service.mettreEnProduction(banque.getId(), liaison, "LIA-2026-002", "PAIEMENT_MOBILE")).isEmpty();
            verify(transitions, never()).save(any());
            verifyNoInteractions(outbox);
        }

        @Test
        @DisplayName("Institution suspendue, à régulariser ou adhérente : signalé, jamais appliqué")
        void autreEtat() {
            for (InstitutionStatus etat : List.of(InstitutionStatus.SUSPENDU, InstitutionStatus.A_REGULARISER, InstitutionStatus.ADHERENT)) {
                banque.setStatus(etat);
                assertThat(service.mettreEnProduction(banque.getId(), liaison, "LIA-2026-003", null)).isEmpty();
                assertThat(banque.getStatus()).isEqualTo(etat);
            }
            verify(transitions, never()).save(any());
            verifyNoInteractions(outbox);
        }

        @Test
        void institutionInconnue() {
            UUID inconnue = UUID.randomUUID();
            assertThatThrownBy(() -> service.mettreEnProduction(inconnue, liaison, "LIA", null)).isInstanceOf(ParticipantException.class);
        }
    }
}
