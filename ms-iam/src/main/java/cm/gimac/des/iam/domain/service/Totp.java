package cm.gimac.des.iam.domain.service;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Code à usage unique fondé sur le temps (TOTP, RFC 6238 sur HOTP, RFC 4226), service de domaine pur
 * (D-65) : secret aléatoire de 20 octets en base 32 (RFC 4648), URI {@code otpauth} du code QR,
 * vérification avec une tolérance d'un pas de part et d'autre. Le secret n'est jamais journalisé.
 */
public final class Totp {

    public static final int OCTETS_SECRET = 20;
    public static final String EMETTEUR = "GIMAC DES";
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int[] PUISSANCES = { 1, 10, 100, 1_000, 10_000, 100_000, 1_000_000, 10_000_000, 100_000_000 };

    private Totp() {}

    /** Nouveau secret aléatoire de 20 octets, en base 32 sans remplissage (32 caractères). */
    public static String nouveauSecret(SecureRandom aleatoire) {
        byte[] octets = new byte[OCTETS_SECRET];
        aleatoire.nextBytes(octets);
        return base32(octets);
    }

    public static String base32(byte[] octets) {
        StringBuilder s = new StringBuilder();
        int tampon = 0;
        int bits = 0;
        for (byte o : octets) {
            tampon = (tampon << 8) | (o & 0xff);
            bits += 8;
            while (bits >= 5) {
                s.append(ALPHABET.charAt((tampon >> (bits - 5)) & 31));
                bits -= 5;
            }
        }
        if (bits > 0) {
            s.append(ALPHABET.charAt((tampon << (5 - bits)) & 31));
        }
        return s.toString();
    }

    /** Décode un secret en base 32 (casse, espaces et remplissage ignorés) ; exception s'il est mal formé. */
    public static byte[] depuisBase32(String texte) {
        String t = texte.replace(" ", "").replace("=", "").toUpperCase(Locale.ROOT);
        if (t.isEmpty()) {
            throw new IllegalArgumentException("secret vide");
        }
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        int tampon = 0;
        int bits = 0;
        for (char c : t.toCharArray()) {
            int v = ALPHABET.indexOf(c);
            if (v < 0) {
                throw new IllegalArgumentException("caractère hors base 32");
            }
            tampon = (tampon << 5) | v;
            bits += 5;
            if (bits >= 8) {
                sortie.write((tampon >> (bits - 8)) & 0xff);
                bits -= 8;
            }
        }
        return sortie.toByteArray();
    }

    /** Code HOTP (RFC 4226) du compteur donné. */
    public static String code(byte[] secret, long compteur, int chiffres, String algorithme) {
        try {
            Mac mac = Mac.getInstance(algorithme);
            mac.init(new SecretKeySpec(secret, algorithme));
            byte[] h = mac.doFinal(ByteBuffer.allocate(8).putLong(compteur).array());
            int decalage = h[h.length - 1] & 0xf;
            int binaire = ((h[decalage] & 0x7f) << 24) | ((h[decalage + 1] & 0xff) << 16) | ((h[decalage + 2] & 0xff) << 8) | (h[decalage + 3] & 0xff);
            return ("%0" + chiffres + "d").formatted(binaire % PUISSANCES[chiffres]);
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Algorithme TOTP non pris en charge : " + algorithme, e);
        }
    }

    /** Code TOTP à l'instant donné. */
    public static String code(String secretBase32, Instant instant, int chiffres, int periode, String algorithme) {
        return code(depuisBase32(secretBase32), Math.floorDiv(instant.getEpochSecond(), periode), chiffres, algorithme);
    }

    /**
     * Vérifie un code à l'instant donné, avec une tolérance de {@code tolerance} pas de part et d'autre
     * (décalage d'horloge du téléphone). Comparaison en temps constant.
     */
    public static boolean verifier(String secretBase32, String code, Instant instant, int chiffres, int periode, String algorithme, int tolerance) {
        if (code == null || !code.matches("\\d{" + chiffres + "}")) {
            return false;
        }
        byte[] secret;
        try {
            secret = depuisBase32(secretBase32);
        } catch (IllegalArgumentException e) {
            return false;
        }
        long pas = Math.floorDiv(instant.getEpochSecond(), periode);
        boolean valide = false;
        for (long p = pas - tolerance; p <= pas + tolerance; p++) {
            valide |= MessageDigest.isEqual(code(secret, p, chiffres, algorithme).getBytes(StandardCharsets.US_ASCII), code.getBytes(StandardCharsets.US_ASCII));
        }
        return valide;
    }

    /**
     * URI du code QR (format « Key Uri » des applications d'authentification) :
     * {@code otpauth://totp/GIMAC%20DES:{identifiant}?secret=…&issuer=GIMAC%20DES&digits=6&period=30&algorithm=SHA1}.
     */
    public static String uri(String identifiant, String secretBase32, int chiffres, int periode, String algorithme) {
        String emetteur = encoder(EMETTEUR);
        return "otpauth://totp/" + emetteur + ":" + encoder(identifiant) + "?secret=" + secretBase32 + "&issuer=" + emetteur + "&digits=" + chiffres + "&period=" + periode
            + "&algorithm=" + algorithme.replaceFirst("(?i)^hmac", "").toUpperCase(Locale.ROOT);
    }

    private static String encoder(String texte) {
        return URLEncoder.encode(texte, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
