package cm.gimac.des.participant.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import cm.gimac.des.participant.domain.enumeration.AdhesionPieceState;
import cm.gimac.des.participant.domain.enumeration.ComplianceState;
import cm.gimac.des.participant.domain.enumeration.InstitutionStatus;
import cm.gimac.des.participant.domain.enumeration.MembershipMode;
import cm.gimac.des.participant.domain.service.PrerequisitesEvaluator.Dossier;
import cm.gimac.des.participant.domain.service.PrerequisitesEvaluator.Marraine;
import cm.gimac.des.participant.domain.service.PrerequisitesEvaluator.Piece;
import cm.gimac.des.participant.domain.service.PrerequisitesEvaluator.Prerequis;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** PAR-03 (test unitaire exigé par le carnet) : prérequis de chaque état d'arrivée. */
class PrerequisitesEvaluatorTest {

    private static final UUID DOC = UUID.randomUUID();

    private static Piece piece(String code, boolean obligatoire, AdhesionPieceState etat) {
        return new Piece(code, "Pièce " + code, obligatoire, etat, etat == AdhesionPieceState.ATTENDUE ? null : DOC);
    }

    private static Dossier dossier(MembershipMode mode, String reglement, String cantonnement, Marraine marraine, List<Piece> pieces) {
        return new Dossier(InstitutionStatus.CANDIDAT, "EP-A-017", mode, reglement, cantonnement, marraine, pieces, null, null, ComplianceState.A_REGULARISER, 0, 0);
    }

    private static List<String> manquants(List<Prerequis> p) {
        return PrerequisitesEvaluator.manquants(p).stream().map(Prerequis::code).toList();
    }

    @Test
    @DisplayName("PAR-01, critère 1 : adhésion directe complète, tous les prérequis satisfaits")
    void adhesionDirecteComplete() {
        Dossier d = dossier(
            MembershipMode.DIRECT,
            "BEAC-001",
            null,
            null,
            List.of(piece("STATUTS", true, AdhesionPieceState.VALIDEE), piece("ORGANIGRAMME", false, AdhesionPieceState.ATTENDUE))
        );
        List<Prerequis> p = PrerequisitesEvaluator.evaluer(d, InstitutionStatus.ADHERENT, null);
        assertThat(manquants(p)).isEmpty();
        assertThat(p).extracting(Prerequis::code).containsExactly("PIECE_STATUTS", "CODE_PARTICIPANT", "MODE_ADHESION", "COMPTE_REGLEMENT");
        assertThat(p.get(0).reference()).isEqualTo(DOC.toString());
    }

    @Test
    @DisplayName("D-57 : une pièce obligatoire non validée bloque l'adhésion ; une pièce facultative non")
    void pieceObligatoireManquante() {
        Dossier d = dossier(
            MembershipMode.DIRECT,
            "BEAC-001",
            null,
            null,
            List.of(piece("STATUTS", true, AdhesionPieceState.RECUE), piece("AGREMENT", true, AdhesionPieceState.ATTENDUE))
        );
        List<Prerequis> p = PrerequisitesEvaluator.evaluer(d, InstitutionStatus.ADHERENT, null);
        assertThat(manquants(p)).containsExactly("PIECE_STATUTS", "PIECE_AGREMENT");
        assertThat(p.get(1).reference()).isEqualTo("ATTENDUE");
    }

    @Test
    @DisplayName("RG-11 : adhésion directe sans compte de règlement refusée ; code et mode exigés")
    void directSansCompte() {
        Dossier d = new Dossier(InstitutionStatus.CANDIDAT, " ", MembershipMode.DIRECT, null, null, null, null, null, null, null, 0, 0);
        assertThat(manquants(PrerequisitesEvaluator.evaluer(d, InstitutionStatus.ADHERENT, null))).containsExactly("CODE_PARTICIPANT", "COMPTE_REGLEMENT");
        Dossier sansMode = new Dossier(InstitutionStatus.CANDIDAT, null, null, null, null, null, List.of(), null, null, null, 0, 0);
        assertThat(manquants(PrerequisitesEvaluator.evaluer(sansMode, InstitutionStatus.ADHERENT, null))).containsExactly("CODE_PARTICIPANT", "MODE_ADHESION");
    }

    @Test
    @DisplayName("PAR-01, critère 2 : adhésion indirecte avec une banque marraine membre direct actif")
    void indirecteAvecMarraineEligible() {
        Dossier d = dossier(MembershipMode.INDIRECT, null, "CANT-9", new Marraine("BQ-B-001", MembershipMode.DIRECT, InstitutionStatus.ACTIF), List.of());
        List<Prerequis> p = PrerequisitesEvaluator.evaluer(d, InstitutionStatus.ADHERENT, null);
        assertThat(manquants(p)).isEmpty();
        assertThat(p).filteredOn(x -> x.code().equals("BANQUE_MARRAINE")).singleElement().extracting(Prerequis::reference).isEqualTo("BQ-B-001");
    }

    @Test
    @DisplayName("PAR-01, critère 4 : banque marraine absente, indirecte ou non active refusée")
    void marraineNonEligible() {
        for (Marraine m : new Marraine[] {
            null,
            new Marraine("X", MembershipMode.INDIRECT, InstitutionStatus.ACTIF),
            new Marraine("Y", MembershipMode.DIRECT, InstitutionStatus.SUSPENDU),
        }) {
            Dossier d = dossier(MembershipMode.INDIRECT, null, null, m, List.of());
            assertThat(manquants(PrerequisitesEvaluator.evaluer(d, InstitutionStatus.ADHERENT, null))).containsExactly(
                "BANQUE_MARRAINE",
                "COMPTE_CANTONNEMENT"
            );
        }
    }

    @Test
    @DisplayName("PAR-03, critère 1 : activation justifiée par l'attestation, la mise en service et la conformité")
    void activation() {
        Dossier ok = new Dossier(InstitutionStatus.EN_HOMOLOGATION, "C", MembershipMode.DIRECT, "R", null, null, List.of(), Instant.EPOCH, Instant.EPOCH, ComplianceState.CONFORME, 1, 0);
        assertThat(manquants(PrerequisitesEvaluator.evaluer(ok, InstitutionStatus.ACTIF, null))).isEmpty();
        Dossier ko = new Dossier(InstitutionStatus.EN_HOMOLOGATION, "C", MembershipMode.DIRECT, "R", null, null, List.of(), null, null, ComplianceState.NON_CONFORME, 1, 0);
        assertThat(manquants(PrerequisitesEvaluator.evaluer(ko, InstitutionStatus.ACTIF, null))).containsExactly(
            "ATTESTATION_RECUE",
            "MISE_EN_SERVICE_AUTORISEE",
            "CONFORMITE"
        );
    }

    @Test
    @DisplayName("PAR-03, critère 2 : la suspension exige la pièce de décision")
    void suspension() {
        Dossier d = dossier(MembershipMode.DIRECT, "R", null, null, List.of());
        assertThat(manquants(PrerequisitesEvaluator.evaluer(d, InstitutionStatus.SUSPENDU, null))).containsExactly("PIECE_DECISION");
        assertThat(manquants(PrerequisitesEvaluator.evaluer(d, InstitutionStatus.SUSPENDU, DOC))).isEmpty();
    }

    @Test
    @DisplayName("Retrait : ni liaison ni suspens ouverts, pièce de décision")
    void retrait() {
        Dossier d = new Dossier(InstitutionStatus.ACTIF, "C", MembershipMode.DIRECT, "R", null, null, List.of(), null, null, null, 2, 1);
        assertThat(manquants(PrerequisitesEvaluator.evaluer(d, InstitutionStatus.RETIRE, null))).containsExactly(
            "AUCUNE_LIAISON_OUVERTE",
            "AUCUN_SUSPENS_OUVERT",
            "PIECE_DECISION"
        );
        Dossier libre = new Dossier(InstitutionStatus.ACTIF, "C", MembershipMode.DIRECT, "R", null, null, List.of(), null, null, null, 0, 0);
        assertThat(manquants(PrerequisitesEvaluator.evaluer(libre, InstitutionStatus.RETIRE, DOC))).isEmpty();
    }

    @Test
    @DisplayName("D-57 : clôture sans suite refusée tant qu'une liaison est ouverte")
    void clotureSansSuite() {
        Dossier ouverte = new Dossier(InstitutionStatus.ADHERENT, "C", MembershipMode.DIRECT, "R", null, null, List.of(), null, null, null, 1, 0);
        assertThat(manquants(PrerequisitesEvaluator.evaluer(ouverte, InstitutionStatus.CLOS_SANS_SUITE, null))).containsExactly("AUCUNE_LIAISON_OUVERTE");
        Dossier libre = new Dossier(InstitutionStatus.CANDIDAT, null, null, null, null, null, List.of(), null, null, null, 0, 0);
        assertThat(manquants(PrerequisitesEvaluator.evaluer(libre, InstitutionStatus.CLOS_SANS_SUITE, null))).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = InstitutionStatus.class, names = { "CANDIDAT", "EN_HOMOLOGATION", "A_REGULARISER" })
    @DisplayName("États sans prérequis propres")
    void sansPrerequis(InstitutionStatus cible) {
        Dossier d = new Dossier(InstitutionStatus.ACTIF, null, null, null, null, null, List.of(), null, null, null, 0, 0);
        assertThat(PrerequisitesEvaluator.evaluer(d, cible, null)).isEmpty();
    }
}
