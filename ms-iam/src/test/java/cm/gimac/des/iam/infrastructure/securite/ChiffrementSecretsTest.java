package cm.gimac.des.iam.infrastructure.securite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Chiffrement AES-256-GCM des secrets gardés par MS-IAM (D-65) : clé fournie ou éphémère, altération détectée. */
class ChiffrementSecretsTest {

    static final String CLE = Base64.getEncoder().encodeToString(new byte[32]);

    @Test
    @DisplayName("Aller-retour, nonce aléatoire, clé fournie ou éphémère ; clé d'une autre taille refusée")
    void allerRetour() {
        ChiffrementSecrets c = new ChiffrementSecrets(CLE);
        String a = c.chiffrer("JBSWY3DPEHPK3PXP");
        assertThat(a).doesNotContain("JBSWY3DPEHPK3PXP").isNotEqualTo(c.chiffrer("JBSWY3DPEHPK3PXP"));
        assertThat(c.dechiffrer(a)).isEqualTo("JBSWY3DPEHPK3PXP");
        assertThat(new ChiffrementSecrets(CLE).dechiffrer(a)).isEqualTo("JBSWY3DPEHPK3PXP");
        ChiffrementSecrets ephemere = new ChiffrementSecrets(" ");
        assertThat(ephemere.dechiffrer(ephemere.chiffrer("é"))).isEqualTo("é");
        assertThatThrownBy(() -> new ChiffrementSecrets(Base64.getEncoder().encodeToString(new byte[16]))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Texte altéré, autre clé ou base 64 invalide : IllegalStateException")
    void alteration() {
        ChiffrementSecrets c = new ChiffrementSecrets(CLE);
        byte[] octets = Base64.getDecoder().decode(c.chiffrer("secret"));
        octets[octets.length - 1] ^= 1;
        assertThatThrownBy(() -> c.dechiffrer(Base64.getEncoder().encodeToString(octets))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new ChiffrementSecrets(null).dechiffrer(c.chiffrer("secret"))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> c.dechiffrer("pas du base 64 !")).isInstanceOf(IllegalStateException.class);
    }
}
