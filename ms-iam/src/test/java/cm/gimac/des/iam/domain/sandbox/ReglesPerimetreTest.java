package cm.gimac.des.iam.domain.sandbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.iam.domain.IamException;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Règles de suspension du périmètre et de protection du dernier administrateur (D-119, US-EXI-06), sans Docker. */
class ReglesPerimetreTest {

    static final Instant T = Instant.parse("2026-10-22T08:00:00Z");

    @Test
    @DisplayName("Code de périmètre : conforme et nettoyé, sinon 400 IAM_SBX_PERIMETRE_INVALIDE")
    void code() {
        assertThat(ReglesPerimetre.code(" 99001 ")).isEqualTo("99001");
        assertThat(ReglesPerimetre.code("2f6c-ab_01")).isEqualTo("2f6c-ab_01");
        for (String invalide : new String[] { null, "", "990 01", "99001/x", "x".repeat(65) }) {
            assertThatThrownBy(() -> ReglesPerimetre.code(invalide)).isInstanceOfSatisfying(IamException.class, e -> {
                assertThat(e.code()).isEqualTo("IAM_SBX_PERIMETRE_INVALIDE");
                assertThat(e.status()).isEqualTo(400);
            });
        }
    }

    @Test
    @DisplayName("US-EXI-06 scénario 2 : l'état effectif superpose l'indicateur sans toucher à l'état du compte")
    void actifEffectif() {
        assertThat(ReglesPerimetre.actifEffectif(true, false)).isTrue();
        assertThat(ReglesPerimetre.actifEffectif(true, true)).isFalse();
        assertThat(ReglesPerimetre.actifEffectif(false, false)).isFalse();
        assertThat(ReglesPerimetre.actifEffectif(false, true)).isFalse();
    }

    @Test
    @DisplayName("US-EXI-06 scénario 3 : la suspension d'un compte protégé est refusée avec motif (409)")
    void protection() {
        assertThatCode(() -> ReglesPerimetre.verifierSuspensionAdmise(false)).doesNotThrowAnyException();
        assertThatThrownBy(() -> ReglesPerimetre.verifierSuspensionAdmise(true)).isInstanceOfSatisfying(IamException.class, e -> {
            assertThat(e.code()).isEqualTo("IAM_SBX_COMPTE_PROTEGE");
            assertThat(e.status()).isEqualTo(409);
            assertThat(e.details()).containsEntry("motif", ReglesPerimetre.MOTIF_PROTECTION);
        });
    }

    @Test
    @DisplayName("Acte d'une institution dont le périmètre est suspendu : 403 IAM_SBX_PERIMETRE_SUSPENDU")
    void perimetreOuvert() {
        assertThatCode(() -> ReglesPerimetre.verifierPerimetreOuvert(false)).doesNotThrowAnyException();
        assertThatThrownBy(() -> ReglesPerimetre.verifierPerimetreOuvert(true)).isInstanceOfSatisfying(IamException.class, e -> assertThat(e.status()).isEqualTo(403));
    }

    @Test
    @DisplayName("Inactivité : dernière connexion, à défaut création, révolue depuis le nombre de jours ; sans date, jamais inactif")
    void inactif() {
        Instant il90 = T.minusSeconds(90L * 86400);
        assertThat(ReglesPerimetre.inactif(il90, null, T, 90)).isTrue();
        assertThat(ReglesPerimetre.inactif(il90.plusSeconds(1), null, T, 90)).isFalse();
        assertThat(ReglesPerimetre.inactif(null, il90, T, 90)).isTrue();
        assertThat(ReglesPerimetre.inactif(T, il90, T, 90)).isFalse();
        assertThat(ReglesPerimetre.inactif(null, null, T, 90)).isFalse();
    }

    @Test
    @DisplayName("Introspection refusée : seul le motif interne est porté, sans portée")
    void introspectionRefusee() {
        Introspection i = Introspection.refusee("s1", "PERIMETRE_SUSPENDU");
        assertThat(i.active()).isFalse();
        assertThat(i.permissions()).isEmpty();
        assertThat(i.composants()).isEmpty();
        assertThat(i.motifRefus()).isEqualTo("PERIMETRE_SUSPENDU");
        assertThat(new Introspection(true, "s", "99001", Set.of("A_B"), Introspection.COMPTE, null, T, null, null).permissions()).containsExactly("A_B");
        assertThat(new Introspection(true, "s", "99001", null, Introspection.COMPTE, null, T, Set.of("SBX-TRACE"), null).composants()).containsExactly("SBX-TRACE");
    }
}
