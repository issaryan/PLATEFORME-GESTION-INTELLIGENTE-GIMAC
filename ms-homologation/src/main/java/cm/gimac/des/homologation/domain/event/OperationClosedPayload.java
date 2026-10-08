package cm.gimac.des.homologation.domain.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

/**
 * Charge de homologation.operation-closed.v1 (sujet homologation.operation, clé = liaison, extension CloudEvents
 * {@code institutionid} = participant) : une opération est close, faite ({@code etat} FAITE : geste, validation,
 * dernière signature, clôture forcée par un événement de MS-CERTIFICATION ou un constat) ou sans objet ({@code etat}
 * SANS_OBJET : instanciation, migration de version). Permet à MS-CERTIFICATION de suivre toutes les clôtures dans sa
 * projection (docs/lot1/effet-reel-referentiels.md §10.5) ; journalisée par l'audit.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OperationClosedPayload(
    UUID liaisonId,
    String reference,
    UUID participantId,
    UUID operationId,
    String code,
    String libelle,
    String etat,
    String declencheur,
    String jalon,
    String lien,
    Instant le
) {
    public static final String TYPE = "homologation.operation-closed.v1";
}
