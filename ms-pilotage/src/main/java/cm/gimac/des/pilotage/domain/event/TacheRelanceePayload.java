package cm.gimac.des.pilotage.domain.event;

import java.time.Instant;
import java.util.UUID;

/** Donnée de pilotage.task-reminded.v1 (sujet pilotage.task, D-81 §6) : relance du responsable d'une tâche, une par 24 h. */
public record TacheRelanceePayload(UUID tacheId, String titre, String responsableId, String acteurId, Instant le) {
    public static final String TYPE = "pilotage.task-reminded.v1";
}
