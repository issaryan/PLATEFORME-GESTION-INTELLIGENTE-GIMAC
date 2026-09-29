package cm.gimac.des.document.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** RG-05 : empreinte SHA-256 calculée par le serveur, en hexadécimal minuscule. */
class EmpreinteSha256Test {

    @Test
    @DisplayName("Vecteurs de la norme FIPS 180-4, en hexadécimal minuscule")
    void vecteurs() {
        assertThat(EmpreinteSha256.de("abc".getBytes(StandardCharsets.US_ASCII))).isEqualTo(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
        );
        assertThat(EmpreinteSha256.de(new byte[0])).isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    @Test
    @DisplayName("Format canonique : 64 caractères hexadécimaux minuscules")
    void format() {
        assertThat(EmpreinteSha256.estValide(EmpreinteSha256.de(new byte[] { 1, 2, 3 }))).isTrue();
        assertThat(EmpreinteSha256.estValide("A".repeat(64))).isFalse();
        assertThat(EmpreinteSha256.estValide("a".repeat(63))).isFalse();
        assertThat(EmpreinteSha256.estValide(null)).isFalse();
    }

    @Test
    @DisplayName("Algorithme inconnu : erreur de configuration explicite")
    void algorithmeInconnu() {
        assertThatThrownBy(() -> EmpreinteSha256.algorithme("SHA-999")).isInstanceOf(IllegalStateException.class).hasMessageContaining("SHA-999");
    }
}
