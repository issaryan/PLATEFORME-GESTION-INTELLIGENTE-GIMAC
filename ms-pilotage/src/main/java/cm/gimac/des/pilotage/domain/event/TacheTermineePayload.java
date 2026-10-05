package cm.gimac.des.pilotage.domain.event;

import java.time.Instant;
import java.util.UUID;

/** Donnée de pilotage.task-completed.v1 : tâche passée à TERMINEE. */
public record TacheTermineePayload(UUID tacheId, String etat, String responsableId, String createurId, String origineType, String origineReference, String acteurId, Instant le) {
    public static final String TYPE = "pilotage.task-completed.v1";
}
