package cm.gimac.des.iam.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.gimac.des.iam.domain.IamException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Règles des permissions de la sandbox (D-115, US-EXI-02) : format, validation réservée au GIMAC, filtre de préfixe. */
class ReglesSandboxTest {

    @Test
    @DisplayName("Format SBX_<MODULE>_<FONCTION> : douze modules, cinq fonctions, rien d'autre")
    void format() {
        assertThat(ReglesSandbox.MODULES).hasSize(12);
        assertThat(ReglesSandbox.FONCTIONS).hasSize(5);
        for (String m : ReglesSandbox.MODULES) {
            for (String f : ReglesSandbox.FONCTIONS) {
                assertThat(ReglesSandbox.conforme("SBX_" + m + "_" + f)).isTrue();
            }
        }
        for (String p : List.of("SBX_XYZ_CONSULTATION", "SBX_MOB_LECTURE", "SBX_MOB", "SBX__CONSULTATION", "HOM_MOB_EXECUTION", "sbx_mob_execution", " SBX_MOB_EXECUTION")) {
            assertThat(ReglesSandbox.conforme(p)).as(p).isFalse();
        }
        assertThat(ReglesSandbox.conforme(null)).isFalse();
        assertThat(ReglesSandbox.sandbox("SBX_ANYTHING")).isTrue();
        assertThat(ReglesSandbox.sandbox("HOM_LIAISON_READ")).isFalse();
        assertThat(ReglesSandbox.sandbox(null)).isFalse();
    }

    @Test
    @DisplayName("Validation de verdict : repérée sur les seules permissions SBX conformes")
    void validation() {
        assertThat(ReglesSandbox.validation("SBX_CER_VALIDATION")).isTrue();
        assertThat(ReglesSandbox.validation("SBX_CER_EXECUTION")).isFalse();
        assertThat(ReglesSandbox.validation("HOM_OPERATION_VALIDATION")).isFalse();
        assertThat(ReglesSandbox.validation("SBX_XYZ_VALIDATION")).isFalse();
    }

    @Test
    @DisplayName("RG-05 : un profil externe ne porte jamais de validation ; un profil interne, si")
    void reservee() {
        assertThatThrownBy(() -> ReglesSandbox.verifierValidationReservee(true, Set.of("SBX_MOB_VALIDATION", "SBX_CER_VALIDATION", "SBX_MOB_EXECUTION")))
            .isInstanceOfSatisfying(IamException.class, e -> {
                assertThat(e.code()).isEqualTo("IAM_SBX_VALIDATION_RESERVEE");
                assertThat(e.status()).isEqualTo(422);
                assertThat(e.details()).containsEntry("permissions", List.of("SBX_CER_VALIDATION", "SBX_MOB_VALIDATION"));
            });
        assertThatCode(() -> ReglesSandbox.verifierValidationReservee(false, Set.of("SBX_CER_VALIDATION"))).doesNotThrowAnyException();
        assertThatCode(() -> ReglesSandbox.verifierValidationReservee(true, Set.of("SBX_CER_EXECUTION"))).doesNotThrowAnyException();
        assertThatCode(() -> ReglesSandbox.verifierValidationReservee(true, null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Filtre de préfixe : insensible à la casse et aux espaces ; absent ou blanc, tout est rendu")
    void filtre() {
        List<String> l = List.of("HOM_LIAISON_READ", "SBX_MOB_EXECUTION", "SBX_CER_VALIDATION");
        assertThat(ReglesSandbox.filtrer(l, " sbx ")).containsExactly("SBX_MOB_EXECUTION", "SBX_CER_VALIDATION");
        assertThat(ReglesSandbox.filtrer(l, null)).isSameAs(l);
        assertThat(ReglesSandbox.filtrer(l, "")).isSameAs(l);
        assertThat(ReglesSandbox.filtrer(l, "PAR")).isEmpty();
    }
}
