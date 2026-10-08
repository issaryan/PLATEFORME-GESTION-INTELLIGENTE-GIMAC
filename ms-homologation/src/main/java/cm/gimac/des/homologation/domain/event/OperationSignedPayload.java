package cm.gimac.des.homologation.domain.event;

import cm.gimac.des.homologation.domain.parcours.SignatureRecueillie;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * homologation.operation-signed.v1 (sujet homologation.operation, D-91 §3) : signature d'une opération d'une
 * liaison, scellée par le registre d'audit. {@code empreinte} : SHA-256 des lignes
 * {@code REFERENCE:documentId:sha256\n} des livrables portés ({@code documents}) ; {@code restantes} : signatures
 * encore attendues ; {@code operationClose} : la dernière signature a clos l'opération. Jamais de contenu de document.
 */
public record OperationSignedPayload(
    UUID liaisonId,
    String reference,
    UUID institutionId,
    UUID operationId,
    String rang,
    String libelle,
    String signature,
    String libelleSignature,
    int ordre,
    String fonction,
    String signataireId,
    String signataireNom,
    String empreinte,
    List<SignatureRecueillie.DocumentSigne> documents,
    List<String> restantes,
    boolean operationClose,
    String lien,
    Instant le
) {
    public static final String TYPE = "homologation.operation-signed.v1";
}
