package cm.gimac.des.pilotage.domain.event;

import java.time.Instant;
import java.util.UUID;

/** Donnée de pilotage.task-created.v1 (sujet pilotage.task, clé = tâche ; CC tableau 6.57). */
public record TacheCreeePayload(
    UUID tacheId,
    String etat,
    String priorite,
    String responsableId,
    String createurId,
    String origineType,
    String origineReference,
    String acteurId,
    Instant le
) {
    public static final String TYPE = "pilotage.task-created.v1";
}
