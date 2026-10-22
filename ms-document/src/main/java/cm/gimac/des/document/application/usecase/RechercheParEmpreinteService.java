package cm.gimac.des.document.application.usecase;

import cm.gimac.des.document.domain.DocumentException;
import cm.gimac.des.document.repository.DepotDocumentRepository;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pièce déjà conservée (D-120, demandée par MS-HOMOLOGATION pour US-EXI-14, critère 3) : document le plus ancien qui
 * porte une empreinte SHA-256 donnée (hexadécimal minuscule, 64 caractères ; 422 sinon ; 404 si aucun).
 */
public class RechercheParEmpreinteService {

    private static final Pattern SHA256 = Pattern.compile("^[0-9a-f]{64}$");

    public record Piece(UUID id, String nomFichier, String typeMime, String sha256) {}

    private final DepotDocumentRepository documents;

    public RechercheParEmpreinteService(DepotDocumentRepository documents) {
        this.documents = documents;
    }

    @Transactional(readOnly = true)
    public Piece parEmpreinte(String sha256) {
        if (sha256 == null || !SHA256.matcher(sha256).matches()) {
            throw DocumentException.refuse("DOC_SHA256_INVALIDE", "Empreinte SHA-256 attendue en hexadécimal minuscule (64 caractères)", java.util.Map.of());
        }
        return documents
            .findFirstBySha256OrderByDeposeLeAsc(sha256)
            .map(d -> new Piece(d.getId(), d.getNomFichier(), d.getTypeMime(), d.getSha256()))
            .orElseThrow(() -> DocumentException.introuvable(sha256));
    }
}
