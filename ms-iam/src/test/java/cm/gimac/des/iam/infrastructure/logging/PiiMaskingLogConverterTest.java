package cm.gimac.des.iam.infrastructure.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Masquage 6+4 des numéros de carte vérifiant Luhn (RG-52, PCI-DSS 3.4). */
class PiiMaskingLogConverterTest {

    @Test
    void masqueEnSixPlusQuatreUnNumeroValide() {
        // Numéro de test public (Visa), valide au sens de Luhn.
        assertThat(PiiMaskingLogConverter.masquerCarte("4111111111111111")).isEqualTo("411111******1111");
        assertThat(PiiMaskingLogConverter.masquerCarte("5555555555554444")).isEqualTo("555555******4444");
    }

    @Test
    void laisseIntacteUneSuiteQuiNeVerifiePasLuhn() {
        assertThat(PiiMaskingLogConverter.masquerCarte("4111111111111112")).isEqualTo("4111111111111112");
        assertThat(PiiMaskingLogConverter.masquerCarte("1790606043198")).isEqualTo("1790606043198");
    }

    @Test
    void formuleDeLuhn() {
        assertThat(PiiMaskingLogConverter.luhnValide("79927398713")).isTrue();
        assertThat(PiiMaskingLogConverter.luhnValide("79927398710")).isFalse();
    }
}
