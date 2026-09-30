package cm.gimac.des.collaboration.infrastructure.crypto;

import cm.gimac.des.collaboration.application.port.out.ChiffrementPort;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Chiffrement au repos des messages (D-64) : AES-256-GCM, vecteur d'initialisation aléatoire de
 * 12 octets par message, étiquette d'authentification de 128 bits, identifiant du message en donnée
 * associée. Format enregistré : version (1 octet) | IV (12 octets) | chiffré et étiquette.
 * La clé (32 octets en base 64) vient de gimac.messagerie.cle-chiffrement (GIMAC_MESSAGERIE_CLE),
 * jamais de la base ; sans clé valide, le service ne démarre pas.
 */
@Component
public class AesGcmChiffrement implements ChiffrementPort {

    static final byte VERSION = 1;
    private static final int IV = 12;
    private static final int ETIQUETTE_BITS = 128;
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private final SecretKey cle;
    private final SecureRandom aleas = new SecureRandom();

    public AesGcmChiffrement(@Value("${gimac.messagerie.cle-chiffrement:}") String cleBase64) {
        byte[] octets;
        try {
            octets = Base64.getDecoder().decode(cleBase64 == null ? "" : cleBase64.strip());
        } catch (IllegalArgumentException e) {
            octets = new byte[0];
        }
        if (octets.length != 32) {
            throw new IllegalStateException(
                "Clé de chiffrement de la messagerie absente ou invalide : gimac.messagerie.cle-chiffrement " +
                "(variable GIMAC_MESSAGERIE_CLE) doit contenir 32 octets encodés en base 64"
            );
        }
        this.cle = new SecretKeySpec(octets, "AES");
    }

    @Override
    public byte[] chiffrer(String clair, UUID messageId) {
        byte[] iv = new byte[IV];
        aleas.nextBytes(iv);
        try {
            Cipher c = Cipher.getInstance(TRANSFORMATION);
            c.init(Cipher.ENCRYPT_MODE, cle, new GCMParameterSpec(ETIQUETTE_BITS, iv));
            c.updateAAD(donneeAssociee(messageId));
            byte[] chiffre = c.doFinal(clair.getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.allocate(1 + IV + chiffre.length).put(VERSION).put(iv).put(chiffre).array();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Chiffrement du message impossible", e);
        }
    }

    @Override
    public String dechiffrer(byte[] chiffre, UUID messageId) {
        if (chiffre == null || chiffre.length < 1 + IV + ETIQUETTE_BITS / 8 || chiffre[0] != VERSION) {
            throw new IllegalStateException("Contenu chiffré du message " + messageId + " illisible (format inconnu)");
        }
        try {
            Cipher c = Cipher.getInstance(TRANSFORMATION);
            c.init(Cipher.DECRYPT_MODE, cle, new GCMParameterSpec(ETIQUETTE_BITS, chiffre, 1, IV));
            c.updateAAD(donneeAssociee(messageId));
            return new String(c.doFinal(chiffre, 1 + IV, chiffre.length - 1 - IV), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Contenu du message " + messageId + " altéré ou clé de chiffrement incorrecte", e);
        }
    }

    private static byte[] donneeAssociee(UUID messageId) {
        return ByteBuffer.allocate(16).putLong(messageId.getMostSignificantBits()).putLong(messageId.getLeastSignificantBits()).array();
    }
}
