package cm.gimac.des.homologation.application.usecase;

import cm.gimac.des.homologation.application.port.out.DocumentRegistryPort;
import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.domain.parcours.DepotLivrable;
import cm.gimac.des.homologation.domain.parcours.OperationParcours;
import cm.gimac.des.homologation.domain.parcours.Parcours;
import cm.gimac.des.homologation.domain.parcours.Preuve;
import cm.gimac.des.homologation.domain.parcours.Vues;
import cm.gimac.des.homologation.domain.service.EtatsPreuves;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pièces et chronologie du dossier d'une liaison (D-109, docs/lot1/referentiel-preuves-signatures.md) : téléchargement
 * d'une pièce rattachée à une opération (preuve ou dépôt d'un livrable), relue chez MS-DOCUMENT et dont l'empreinte
 * SHA-256 est recalculée et comparée à celle inscrite au dépôt ; chronologie réelle lue dans l'état persistant.
 */
@Service
@Transactional(readOnly = true)
public class PiecesParcours {

    /** Pièce servie : contenu, nom, type, empreinte inscrite au dépôt et empreinte recalculée (égales). */
    public record PieceServie(byte[] octets, String nomFichier, String typeMime, String empreinte) {}

    private final ParcoursDepot depot;
    private final DocumentRegistryPort documents;

    public PiecesParcours(ParcoursDepot depot, DocumentRegistryPort documents) {
        this.depot = depot;
        this.documents = documents;
    }

    /**
     * Pièce d'une opération : la référence doit être celle de sa preuve ou d'un de ses dépôts (404 HOM_PIECE_INCONNUE),
     * connue de MS-DOCUMENT (404) et d'empreinte identique à celle inscrite (422 HOM_DOCUMENT_HASH_MISMATCH).
     */
    public PieceServie piece(UUID operationId, String documentId) {
        Parcours p = depot.deLOperationLecture(operationId);
        OperationParcours o = p.operation(operationId);
        String attendue = empreinteInscrite(o, documentId);
        if (attendue == null) {
            throw HomologationException.notFound("HOM_PIECE_INCONNUE", "Cette pièce n'est pas rattachée à l'opération.", documentId);
        }
        UUID id;
        try {
            id = UUID.fromString(documentId);
        } catch (IllegalArgumentException e) {
            throw HomologationException.notFound("HOM_PIECE_INCONNUE", "Cette pièce n'est pas tenue par le registre documentaire.", documentId);
        }
        DocumentRegistryPort.Contenu c = documents
            .lire(id)
            .orElseThrow(() -> HomologationException.notFound("HOM_PIECE_INCONNUE", "Cette pièce est inconnue du registre documentaire.", documentId));
        String calculee = sha256(c.octets());
        if (!calculee.equalsIgnoreCase(attendue)) {
            throw HomologationException.unprocessable("HOM_DOCUMENT_HASH_MISMATCH", "L'empreinte de la pièce ne correspond pas à celle inscrite au dépôt.");
        }
        return new PieceServie(c.octets(), c.nomFichier(), c.typeMime(), calculee);
    }

    /** Empreinte inscrite pour une pièce de l'opération, ou null si la pièce ne lui est pas rattachée. */
    static String empreinteInscrite(OperationParcours o, String documentId) {
        if (documentId == null || documentId.isBlank()) {
            return null;
        }
        for (DepotLivrable d : o.getDepots()) {
            if (documentId.equals(d.documentRef()) && d.empreinte() != null) {
                return d.empreinte();
            }
        }
        Preuve pr = o.getPreuve();
        return pr != null && documentId.equals(pr.documentRef()) ? pr.empreinte() : null;
    }

    /** Chronologie réelle de la liaison (dépôts, sans objet, renvois, signatures, clôtures, reprises, blocages). */
    public List<Vues.EvenementChronologie> chronologie(UUID liaisonId) {
        depot.liaison(liaisonId);
        return depot.trouver(liaisonId, false).map(EtatsPreuves::chronologie).orElse(List.of());
    }

    static String sha256(byte[] contenu) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenu)).toLowerCase(Locale.ROOT);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }
}
