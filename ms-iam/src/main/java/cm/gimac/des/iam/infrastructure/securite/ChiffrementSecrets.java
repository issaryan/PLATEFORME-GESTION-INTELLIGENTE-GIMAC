package cm.gimac.des.iam.infrastructure.securite;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Chiffrement des secrets gardés par MS-IAM (D-65) : AES-256-GCM, nonce aléatoire de 12 octets en
 * tête, le tout en base 64. La clé (32 octets en base 64, gimac.iam.chiffrement.cle) est fournie par
 * l'environnement ; à défaut, une clé éphémère est tirée au démarrage (développement et test : les
 * secrets ne survivent pas au redémarrage), avec un avertissement.
 */
public class ChiffrementSecrets {

    private static final Logger LOG = LoggerFactory.getLogger(ChiffrementSecrets.class);
    private static final String ALGORITHME = "AES/GCM/NoPadding";
    private static final int NONCE = 12;
    private static final int ETIQUETTE_BITS = 128;

    private final SecretKeySpec cle;
    private final SecureRandom aleatoire = new SecureRandom();

    public ChiffrementSecrets(String cleBase64) {
        byte[] octets;
        if (cleBase64 == null || cleBase64.isBlank()) {
            LOG.warn("gimac.iam.chiffrement.cle absente : clé de chiffrement éphémère (secrets perdus au redémarrage), à fournir en production");
            octets = new byte[32];
            aleatoire.nextBytes(octets);
        } else {
            octets = Base64.getDecoder().decode(cleBase64.trim());
            if (octets.length != 32) {
                throw new IllegalArgumentException("gimac.iam.chiffrement.cle : 32 octets en base 64 attendus (AES-256)");
            }
        }
        this.cle = new SecretKeySpec(octets, "AES");
    }

    public String chiffrer(String clair) {
        try {
            byte[] nonce = new byte[NONCE];
            aleatoire.nextBytes(nonce);
            Cipher c = Cipher.getInstance(ALGORITHME);
            c.init(Cipher.ENCRYPT_MODE, cle, new GCMParameterSpec(ETIQUETTE_BITS, nonce));
            byte[] chiffre = c.doFinal(clair.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(NONCE + chiffre.length).put(nonce).put(chiffre).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Chiffrement impossible", e);
        }
    }

    /** Déchiffre ; IllegalStateException si le texte a été altéré ou chiffré avec une autre clé. */
    public String dechiffrer(String chiffre) {
        try {
            byte[] tout = Base64.getDecoder().decode(chiffre);
            Cipher c = Cipher.getInstance(ALGORITHME);
            c.init(Cipher.DECRYPT_MODE, cle, new GCMParameterSpec(ETIQUETTE_BITS, tout, 0, NONCE));
            return new String(c.doFinal(tout, NONCE, tout.length - NONCE), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Secret illisible (clé différente ou donnée altérée)", e);
        }
    }
}
