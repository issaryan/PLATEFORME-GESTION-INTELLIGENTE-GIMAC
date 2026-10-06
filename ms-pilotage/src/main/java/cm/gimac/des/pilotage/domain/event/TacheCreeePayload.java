package cm.gimac.des.pilotage.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Donnée de pilotage.task-created.v1 (sujet pilotage.task, clé = tâche ; CC tableau 6.57). Tâche confiée à
 * une unité (D-81 §6) : {@code responsableId} null, {@code uniteCible} renseignée ; tâche d'un envoi à
 * plusieurs personnes : {@code groupeId}.
 */
public record TacheCreeePayload(
    UUID tacheId,
    String etat,
    String priorite,
    String responsableId,
    String createurId,
    String origineType,
    String origineReference,
    String acteurId,
    Instant le,
    UUID groupeId,
    String uniteCible
) {
    public static final String TYPE = "pilotage.task-created.v1";
}
