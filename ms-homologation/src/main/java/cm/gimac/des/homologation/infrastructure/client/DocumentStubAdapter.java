package cm.gimac.des.homologation.infrastructure.client;

import cm.gimac.des.homologation.application.port.out.DocumentRegistryPort;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Adaptateur de substitution du registre documentaire (développement, tests) : les pièces déposées ici
 * sont retenues avec leur empreinte ; une pièce inconnue portant une empreinte bien formée est admise,
 * une pièce déclarée absente ({@link #retirer}) est inconnue.
 */
@Component
@ConditionalOnProperty(name = "gimac.adapters.document-deposit", havingValue = "stub", matchIfMissing = true)
public class DocumentStubAdapter implements DocumentRegistryPort {

    private static final Pattern SHA256 = Pattern.compile("^[0-9a-fA-F]{64}$");
    private final Map<UUID, Optional<String>> pieces = new ConcurrentHashMap<>();
    private final Map<UUID, String> types = new ConcurrentHashMap<>();

    /** Type d'une pièce (par défaut application/pdf, comme les livrables de l'annexe A). */
    public void typer(UUID id, String typeMime) {
        types.put(id, typeMime);
    }

    @Override
    public String typeMime(UUID documentId) {
        return types.getOrDefault(documentId, "application/pdf");
    }

    /** Enregistre une pièce et son empreinte, comme MS-DOCUMENT après un dépôt. */
    public void enregistrer(UUID id, String sha256) {
        pieces.put(id, Optional.of(sha256));
    }

    /** Pièce absente du registre. */
    public void retirer(UUID id) {
        pieces.put(id, Optional.empty());
    }

    @Override
    public Verification verifier(UUID documentId, String sha256) {
        Optional<String> connue = pieces.get(documentId);
        if (connue == null) {
            return sha256 != null && SHA256.matcher(sha256).matches() ? Verification.CONFORME : Verification.DIVERGENTE;
        }
        return connue.map(e -> e.equalsIgnoreCase(sha256) ? Verification.CONFORME : Verification.DIVERGENTE).orElse(Verification.INCONNUE);
    }

    @Override
    public Depot deposer(byte[] contenu, String nomFichier, String typeMime, String finalite, String reference, UUID deposantId) {
        UUID id = UUID.randomUUID();
        String empreinte = sha256(contenu);
        enregistrer(id, empreinte);
        if (typeMime != null) {
            typer(id, typeMime);
        }
        return new Depot(id, empreinte, nomFichier);
    }

    static String sha256(byte[] contenu) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenu));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }
}
