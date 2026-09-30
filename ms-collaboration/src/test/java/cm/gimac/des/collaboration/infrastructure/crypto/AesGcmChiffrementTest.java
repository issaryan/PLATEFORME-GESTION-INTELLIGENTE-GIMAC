package cm.gimac.des.collaboration.infrastructure.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Chiffrement au repos des messages (D-64) : AES-256-GCM, clé hors base, message lié au chiffré. */
class AesGcmChiffrementTest {

    static final String CLE = Base64.getEncoder().encodeToString("cle-de-test-messagerie-gimac-D64".getBytes(StandardCharsets.US_ASCII));
    final AesGcmChiffrement chiffrement = new AesGcmChiffrement(CLE);

    @Test
    @DisplayName("Aller-retour ; IV aléatoire : deux chiffrés d'un même texte diffèrent ; aucun clair visible")
    void allerRetour() {
        UUID id = UUID.randomUUID();
        String clair = "Réunion de recette à 10 h — pièces jointes à venir";
        byte[] a = chiffrement.chiffrer(clair, id);
        byte[] b = chiffrement.chiffrer(clair, id);
        assertThat(a[0]).isEqualTo(AesGcmChiffrement.VERSION);
        assertThat(a).isNotEqualTo(b);
        assertThat(new String(a, StandardCharsets.ISO_8859_1)).doesNotContain("Réunion", "recette");
        assertThat(chiffrement.dechiffrer(a, id)).isEqualTo(clair);
        assertThat(chiffrement.dechiffrer(chiffrement.chiffrer("", id), id)).isEmpty();
    }

    @Test
    @DisplayName("Chiffré altéré, déplacé vers un autre message ou d'une autre clé : refusé")
    void integrite() {
        UUID id = UUID.randomUUID();
        byte[] c = chiffrement.chiffrer("secret", id);
        UUID autre = UUID.randomUUID();
        assertThatThrownBy(() -> chiffrement.dechiffrer(c, autre)).isInstanceOf(IllegalStateException.class).hasMessageContaining("altéré");
        byte[] altere = c.clone();
        altere[altere.length - 1] ^= 1;
        assertThatThrownBy(() -> chiffrement.dechiffrer(altere, id)).isInstanceOf(IllegalStateException.class);
        var autreCle = new AesGcmChiffrement(Base64.getEncoder().encodeToString(new byte[32]));
        assertThatThrownBy(() -> autreCle.dechiffrer(c, id)).isInstanceOf(IllegalStateException.class);
        byte[] version = c.clone();
        version[0] = 9;
        assertThatThrownBy(() -> chiffrement.dechiffrer(version, id)).hasMessageContaining("format inconnu");
        assertThatThrownBy(() -> chiffrement.dechiffrer(new byte[5], id)).hasMessageContaining("format inconnu");
        assertThatThrownBy(() -> chiffrement.dechiffrer(null, id)).hasMessageContaining("format inconnu");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "pas du base64 !", "AAAA", "  " })
    @DisplayName("Sans clé de 32 octets en base 64, le service ne démarre pas")
    void cleInvalide(String cle) {
        assertThatThrownBy(() -> new AesGcmChiffrement(cle)).isInstanceOf(IllegalStateException.class).hasMessageContaining("GIMAC_MESSAGERIE_CLE");
    }
}
