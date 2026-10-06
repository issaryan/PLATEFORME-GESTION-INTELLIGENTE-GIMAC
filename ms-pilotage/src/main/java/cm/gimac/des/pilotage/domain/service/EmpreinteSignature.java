package cm.gimac.des.pilotage.domain.service;

import cm.gimac.des.pilotage.domain.processus.DocumentSigne;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Empreinte signée d'une signature d'étape (§2) : SHA-256 (hexadécimal, minuscules) de la suite des lignes
 * {@code CODE_LIVRABLE:documentId:sha256} des livrables portés, dans l'ordre de la définition, séparées par un
 * saut de ligne. Une même empreinte se recalcule à partir des documents conservés.
 */
public final class EmpreinteSignature {

    private static final Pattern SHA256 = Pattern.compile("^[0-9a-f]{64}$");

    private EmpreinteSignature() {}

    public static String calculer(List<DocumentSigne> documents) {
        StringBuilder sb = new StringBuilder();
        for (DocumentSigne d : documents) {
            sb.append(d.livrable()).append(':').append(d.documentId()).append(':').append(d.sha256()).append('\n');
        }
        return sha256(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    public static String sha256(byte[] octets) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(octets));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }

    /** Empreinte SHA-256 hexadécimale (64 caractères, minuscules après normalisation). */
    public static boolean valide(String empreinte) {
        return empreinte != null && SHA256.matcher(empreinte).matches();
    }
}
