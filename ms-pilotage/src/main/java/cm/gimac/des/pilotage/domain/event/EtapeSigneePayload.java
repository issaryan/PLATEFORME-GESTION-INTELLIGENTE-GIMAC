package cm.gimac.des.pilotage.domain.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Donnée de pilotage.process-step-signed.v1 (sujet pilotage.process, clé = instance, D-87) : signature apposée
 * sur l'étape {@code etape} (libellé {@code libelleEtape}) après vérification FIDO2 (finalité SIGNATURE_PROCESSUS) ; {@code empreinte} : SHA-256 signée des livrables
 * portés ({@code documents} : livrable, document, SHA-256), que le registre d'audit scelle ; {@code etatEtape} :
 * état de l'étape après la signature ; {@code destinataires} : signataires suivants, ou validateur, ou exécutant
 * si l'étape est terminée.
 */
public record EtapeSigneePayload(
    UUID instanceId,
    String code,
    String intitule,
    String etape,
    String libelleEtape,
    String signature,
    String signataireId,
    String empreinte,
    List<Document> documents,
    String etatEtape,
    List<String> destinataires,
    String lien,
    Instant le
) {
    public static final String TYPE = "pilotage.process-step-signed.v1";

    public record Document(String livrable, String documentId, String sha256) {}

    public EtapeSigneePayload {
        documents = documents == null ? List.of() : List.copyOf(documents);
        destinataires = destinataires == null ? List.of() : List.copyOf(destinataires);
    }
}
