package cm.gimac.des.iam.domain.sandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.sandbox.Delegation.EtatDelegation;
import cm.gimac.des.iam.domain.sandbox.Delegation.EtatEffectif;
import cm.gimac.des.iam.domain.sandbox.Delegation.TypeDelegation;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Bornes de date et de durée des délégations de la sandbox (D-119, US-EXI-04, US-EXI-24), sans Docker. */
class ReglesDelegationTest {

    static final Instant T = Instant.parse("2026-10-22T08:00:00Z");
    static final Duration MAX = Duration.ofDays(90);

    static void refus(Runnable r, String code, int statut) {
        assertThatThrownBy(r::run).isInstanceOfSatisfying(IamException.class, e -> {
            assertThat(e.code()).isEqualTo(code);
            assertThat(e.status()).isEqualTo(statut);
        });
    }

    static Delegation d(EtatDelegation etat, Instant debut, Instant fin) {
        return new Delegation("d1", TypeDelegation.MANDAT, "c1", "99001", Set.of("SBX_RAC_CONSULTATION"), null, debut, fin, etat, "sa", null, "m", T, null);
    }

    @Test
    @DisplayName("État effectif : programmé, actif, échu à la date de fin exactement ; révoqué, refusé, demandé")
    void etatEffectif() {
        Instant debut = T.plusSeconds(60);
        Instant fin = T.plusSeconds(120);
        assertThat(ReglesDelegation.etatEffectif(EtatDelegation.ACCORDEE, debut, fin, T)).isEqualTo(EtatEffectif.PROGRAMMEE);
        assertThat(ReglesDelegation.etatEffectif(EtatDelegation.ACCORDEE, debut, fin, debut)).isEqualTo(EtatEffectif.ACTIVE);
        assertThat(ReglesDelegation.etatEffectif(EtatDelegation.ACCORDEE, debut, fin, fin)).isEqualTo(EtatEffectif.ECHUE);
        assertThat(ReglesDelegation.etatEffectif(EtatDelegation.REVOQUEE, debut, fin, T)).isEqualTo(EtatEffectif.REVOQUEE);
        assertThat(ReglesDelegation.etatEffectif(EtatDelegation.REFUSEE, debut, fin, T)).isEqualTo(EtatEffectif.REFUSEE);
        assertThat(ReglesDelegation.etatEffectif(EtatDelegation.DEMANDEE, debut, fin, T)).isEqualTo(EtatEffectif.DEMANDEE);
        assertThat(ReglesDelegation.etatEffectif(EtatDelegation.DEMANDEE, debut, fin, fin)).isEqualTo(EtatEffectif.ECHUE);
    }

    @Test
    @DisplayName("US-EXI-04 scénario 4 : durée supérieure au maximum paramétré refusée (problème HTTP normalisé, 422)")
    void dureeExcessive() {
        assertThatCode(() -> ReglesDelegation.verifierBornes(T, T.plus(MAX), T, MAX)).doesNotThrowAnyException();
        assertThatThrownBy(() -> ReglesDelegation.verifierBornes(T, T.plus(MAX).plusSeconds(1), T, MAX)).isInstanceOfSatisfying(IamException.class, e -> {
            assertThat(e.code()).isEqualTo("IAM_SBX_DUREE_EXCESSIVE");
            assertThat(e.status()).isEqualTo(422);
            assertThat(e.details()).containsEntry("dureeMax", "PT2160H");
        });
    }

    @Test
    @DisplayName("Bornes : début avant la fin, fin à venir, dates exigées")
    void bornes() {
        refus(() -> ReglesDelegation.verifierBornes(T, T, T, MAX), "IAM_SBX_PERIODE_INVALIDE", 422);
        refus(() -> ReglesDelegation.verifierBornes(null, T, T, MAX), "IAM_SBX_PERIODE_INVALIDE", 422);
        refus(() -> ReglesDelegation.verifierBornes(T, null, T, MAX), "IAM_SBX_PERIODE_INVALIDE", 422);
        refus(() -> ReglesDelegation.verifierBornes(T.minusSeconds(120), T.minusSeconds(60), T, MAX), "IAM_SBX_PERIODE_ECHUE", 422);
        refus(() -> ReglesDelegation.verifierBornes(T.minusSeconds(120), T, T, MAX), "IAM_SBX_PERIODE_ECHUE", 422);
    }

    @Test
    @DisplayName("Périmètre d'un mandat : droits de portée institution seulement, jamais de validation, au moins un")
    void perimetre() {
        Set<String> accordables = ReglesDelegation.accordables(
            Set.of("ROLE_SBX_DEVELOPPEUR", "ROLE_SBX_INCONNU"),
            Map.of("ROLE_SBX_DEVELOPPEUR", Set.of("SBX_RAC_ADMINISTRATION", "SBX_CER_VALIDATION", "NOT_INBOX_READ", "SBX_XYZ_LECTURE"), "ROLE_SNA", Set.of("SBX_CER_EXECUTION"))
        );
        assertThat(accordables).containsExactly("SBX_RAC_ADMINISTRATION");
        assertThat(ReglesDelegation.verifierPerimetre(List.of("SBX_RAC_ADMINISTRATION"), accordables)).containsExactly("SBX_RAC_ADMINISTRATION");
        refus(() -> ReglesDelegation.verifierPerimetre(List.of(), accordables), "IAM_SBX_PERIMETRE_REQUIS", 400);
        refus(() -> ReglesDelegation.verifierPerimetre(null, accordables), "IAM_SBX_PERIMETRE_REQUIS", 400);
        refus(() -> ReglesDelegation.verifierPerimetre(List.of("SBX_CER_VALIDATION"), accordables), "IAM_SBX_VALIDATION_RESERVEE", 422);
        refus(() -> ReglesDelegation.verifierPerimetre(List.of("SBX_CER_EXECUTION"), accordables), "IAM_SBX_PERIMETRE_HORS_PORTEE", 403);
    }

    @Test
    @DisplayName("Composants d'un accès de prestataire : au moins un, tous connus")
    void composants() {
        assertThat(ReglesDelegation.verifierComposants(List.of("SBX-TRACE"))).containsExactly("SBX-TRACE");
        assertThat(ReglesDelegation.COMPOSANTS).hasSize(12);
        refus(() -> ReglesDelegation.verifierComposants(List.of()), "IAM_SBX_COMPOSANT_REQUIS", 400);
        refus(() -> ReglesDelegation.verifierComposants(null), "IAM_SBX_COMPOSANT_REQUIS", 400);
        refus(() -> ReglesDelegation.verifierComposants(List.of("SBX-TRACE", "ORACLE")), "IAM_SBX_COMPOSANT_INCONNU", 400);
    }

    @Test
    @DisplayName("Prolongation : délégation en cours, nouvelle fin plus lointaine, durée totale bornée")
    void prolongation() {
        Delegation active = d(EtatDelegation.ACCORDEE, T.minusSeconds(60), T.plusSeconds(60));
        assertThatCode(() -> ReglesDelegation.verifierProlongation(active, T.plusSeconds(120), T, MAX)).doesNotThrowAnyException();
        assertThatCode(() -> ReglesDelegation.verifierProlongation(d(EtatDelegation.ACCORDEE, T.plusSeconds(60), T.plusSeconds(120)), T.plusSeconds(180), T, MAX))
            .doesNotThrowAnyException();
        refus(() -> ReglesDelegation.verifierProlongation(active, T.plusSeconds(60), T, MAX), "IAM_SBX_PROLONGATION_INVALIDE", 422);
        refus(() -> ReglesDelegation.verifierProlongation(active, null, T, MAX), "IAM_SBX_PROLONGATION_INVALIDE", 422);
        refus(() -> ReglesDelegation.verifierProlongation(active, T.minusSeconds(60).plus(MAX).plusSeconds(1), T, MAX), "IAM_SBX_DUREE_EXCESSIVE", 422);
        refus(() -> ReglesDelegation.verifierProlongation(d(EtatDelegation.REVOQUEE, T.minusSeconds(60), T.plusSeconds(60)), T.plusSeconds(120), T, MAX), "IAM_SBX_DELEGATION_CLOSE", 409);
        refus(() -> ReglesDelegation.verifierProlongation(d(EtatDelegation.ACCORDEE, T.minusSeconds(60), T), T.plusSeconds(120), T, MAX), "IAM_SBX_DELEGATION_CLOSE", 409);
    }

    @Test
    @DisplayName("Demande d'accès : décidée une seule fois ; transitions conservant l'approbateur et la clôture")
    void demande() {
        Delegation demandee = new Delegation("d2", TypeDelegation.PRESTATAIRE, "c2", null, null, Set.of("SBX-TRACE"), T, T.plusSeconds(3600), EtatDelegation.DEMANDEE, "u1", null, "maintenance", T, null);
        assertThatCode(() -> ReglesDelegation.verifierDemandeOuverte(demandee, T)).doesNotThrowAnyException();
        Delegation accordee = demandee.avecEtat(EtatDelegation.ACCORDEE, "rssi", null, T);
        assertThat(accordee.approbateurId()).isEqualTo("rssi");
        assertThat(accordee.motif()).isEqualTo("maintenance");
        assertThat(accordee.closeLe()).isNull();
        refus(() -> ReglesDelegation.verifierDemandeOuverte(accordee, T), "IAM_SBX_DELEGATION_CLOSE", 409);
        Delegation refusee = demandee.avecEtat(EtatDelegation.REFUSEE, "rssi", "non", T);
        assertThat(refusee.closeLe()).isEqualTo(T);
        assertThat(refusee.approbateurId()).isNull();
        Delegation revoquee = accordee.avecEtat(EtatDelegation.REVOQUEE, "rssi", "fin", T);
        assertThat(revoquee.closeLe()).isEqualTo(T);
        assertThat(revoquee.approbateurId()).isEqualTo("rssi");
        Delegation mandat = d(EtatDelegation.ACCORDEE, T, T.plusSeconds(60)).avecEtat(EtatDelegation.ACCORDEE, "x", null, T);
        assertThat(mandat.approbateurId()).isNull();
        assertThat(mandat.avecFin(T.plusSeconds(120)).fin()).isEqualTo(T.plusSeconds(120));
        assertThat(mandat.composants()).isEmpty();
        assertThat(demandee.permissions()).isEmpty();
    }
}
