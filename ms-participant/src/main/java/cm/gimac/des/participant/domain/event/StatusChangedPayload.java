package cm.gimac.des.participant.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * participant.status-changed.v1 (D-37) : statut d'une institution modifié après approbation.
 * Les champs institutionId, statut et motif sont ceux lus par MS-HOMOLOGATION (HOM-06).
 * versionStatutRegistre (D-120, US-EXI-20) : version de statut strictement croissante par institution, ajoutée par
 * extension (champ optionnel pour les lecteurs : absent des événements publiés avant D-120) ; contrat lisible par
 * machine : config/contrats/participant.status-changed.v1.schema.json.
 */
public record StatusChangedPayload(
    UUID institutionId,
    String participantCode,
    String ancienStatut,
    String statut,
    String motif,
    UUID transitionId,
    Instant dateEffet,
    long versionStatutRegistre
) {
    public static final String TYPE = "participant.status-changed.v1";
}
