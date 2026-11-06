package cm.gimac.des.pilotage.domain.processus;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CodeProcessusTest {

    @Test
    void uniteNatureEtNumeroDansQuaranteCaracteres() {
        assertThat(CodeProcessus.suivant("SSI", 12)).isEqualTo("SSI-PRC-12");
        assertThat(CodeProcessus.suivant("POLE_RESEAU", 1)).isEqualTo("POLE_RESEAU-PRC-1");
        String longue = "UNITE_AU_NOM_TRES_LONG_DE_32_CAR";
        assertThat(longue).hasSize(32);
        String code = CodeProcessus.suivant(longue, 1_234_567);
        assertThat(code).hasSize(CodeProcessus.LONGUEUR_MAX).endsWith("-PRC-1234567").startsWith("UNITE_AU_NOM_TRES_LONG_D");
        assertThat(code).matches("^[A-Z][A-Z0-9_-]{1,39}$");
    }
}
