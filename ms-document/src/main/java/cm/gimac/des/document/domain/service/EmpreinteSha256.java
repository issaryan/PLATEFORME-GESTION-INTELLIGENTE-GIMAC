package cm.gimac.des.document.domain.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;

/**
 * Empreinte SHA-256 d'un contenu déposé, calculée par le serveur et stockée en hexadécimal
 * minuscule (64 caractères), convention du modèle de MS-DOCUMENT (RG-05).
 */
public final class EmpreinteSha256 {

    private static final Pattern FORMAT = Pattern.compile("^[0-9a-f]{64}$");

    private EmpreinteSha256() {}

    /** Empreinte du contenu, en hexadécimal minuscule. */
    public static String de(byte[] contenu) {
        return HexFormat.of().formatHex(algorithme("SHA-256").digest(contenu));
    }

    /** Vrai pour une empreinte au format canonique (64 caractères hexadécimaux minuscules). */
    public static boolean estValide(String empreinte) {
        return empreinte != null && FORMAT.matcher(empreinte).matches();
    }

    static MessageDigest algorithme(String nom) {
        try {
            return MessageDigest.getInstance(nom);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 est exigé de toute plateforme Java (JCA) : cas réservé à un nom erroné.
            throw new IllegalStateException(nom + " indisponible", e);
        }
    }
}
