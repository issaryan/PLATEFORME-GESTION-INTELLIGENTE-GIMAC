package cm.gimac.des.homologation.domain.parcours;

import java.time.Instant;
import java.util.List;

/**
 * Signature recueillie sur une opération d'une liaison (D-91 §3) : signataire (compte et fonction), date, empreinte
 * SHA-256 signée (lignes {@code REFERENCE:documentId:sha256\n} des livrables portés déposés, dans l'ordre de
 * {@code porte}) et documents correspondants. Scellée au registre d'audit par l'événement
 * {@code homologation.operation-signed.v1}. {@code origine} : null pour une signature donnée dans MS-HOMOLOGATION ;
 * type de l'événement pour une signature recueillie par un autre service et reprise de lui (D-94 : signature du
 * procès-verbal lue dans certification.pv-signed.v1, déjà scellée par MS-CERTIFICATION, jamais redemandée).
 */
public record SignatureRecueillie(
    String code,
    String signataireId,
    String signataireNom,
    Fonction fonction,
    Instant signeLe,
    String empreinte,
    List<DocumentSigne> documents,
    String origine
) {
    public SignatureRecueillie {
        documents = documents == null ? List.of() : List.copyOf(documents);
    }

    public SignatureRecueillie(String code, String signataireId, String signataireNom, Fonction fonction, Instant signeLe, String empreinte, List<DocumentSigne> documents) {
        this(code, signataireId, signataireNom, fonction, signeLe, empreinte, documents, null);
    }

    /** Livrable porté par la signature : référence, document de MS-DOCUMENT et son empreinte. */
    public record DocumentSigne(String livrable, String documentId, String sha256) {}
}
