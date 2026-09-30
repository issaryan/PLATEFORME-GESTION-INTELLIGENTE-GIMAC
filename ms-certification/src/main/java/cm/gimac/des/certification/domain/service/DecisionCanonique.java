package cm.gimac.des.certification.domain.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Forme canonique d'une décision et son empreinte SHA-256 (D-35) : champs dans un ordre fixe, une ligne
 * « clé=valeur » par champ, valeur absente écrite vide. L'empreinte de la décision est ce que le cachet de
 * la plateforme contresigne, avec l'assertion du signataire.
 */
public final class DecisionCanonique {

    private final Map<String, String> champs = new LinkedHashMap<>();

    private DecisionCanonique() {}

    public static DecisionCanonique de(String nature) {
        return new DecisionCanonique().avec("nature", nature);
    }

    public DecisionCanonique avec(String cle, Object valeur) {
        champs.put(cle, valeur == null ? "" : String.valueOf(valeur).replace('\n', ' '));
        return this;
    }

    public String texte() {
        StringBuilder sb = new StringBuilder();
        champs.forEach((k, v) -> sb.append(k).append('=').append(v).append('\n'));
        return sb.toString();
    }

    public String empreinte() {
        return sha256(texte());
    }

    /** Empreinte SHA-256 en hexadécimal minuscule. */
    public static String sha256(String texte) {
        return sha256(texte.getBytes(StandardCharsets.UTF_8));
    }

    public static String sha256(byte[] octets) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(octets));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }
}
