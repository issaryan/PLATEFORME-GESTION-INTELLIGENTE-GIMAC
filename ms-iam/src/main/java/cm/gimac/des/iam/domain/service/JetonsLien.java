package cm.gimac.des.iam.domain.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Jetons des liens envoyés par courriel (D-65), service de domaine pur : 32 octets aléatoires en
 * base64url ; seule l'empreinte SHA-256 est conservée, le jeton en clair ne quitte que le courriel.
 */
public final class JetonsLien {

    public static final int OCTETS = 32;

    private JetonsLien() {}

    public static String nouveau(SecureRandom aleatoire) {
        byte[] octets = new byte[OCTETS];
        aleatoire.nextBytes(octets);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(octets);
    }

    /** Empreinte SHA-256, en hexadécimal, sous laquelle le jeton est conservé et retrouvé. */
    public static String empreinte(String jeton) {
        return HexFormat.of().formatHex(condensat("SHA-256").digest(jeton.getBytes(StandardCharsets.UTF_8)));
    }

    static MessageDigest condensat(String algorithme) {
        try {
            return MessageDigest.getInstance(algorithme);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(algorithme + " indisponible", e);
        }
    }

    /** Forme d'un jeton émis (43 caractères base64url) ; tout autre texte est refusé sans consultation. */
    public static boolean bienForme(String jeton) {
        return jeton != null && jeton.matches("^[A-Za-z0-9_-]{43}$");
    }
}
