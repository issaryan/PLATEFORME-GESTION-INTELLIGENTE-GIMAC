package cm.gimac.des.pilotage.domain.event;

import java.time.Instant;
import java.util.UUID;

/** Donnée de pilotage.task-assigned.v1 : tâche confiée à un autre que son créateur, ou réaffectée. */
/** {@code titre} et {@code echeance} (AAAA-MM-JJ ou null) servent au titre de la notification (D-80). */
public record TacheAffecteePayload(
    UUID tacheId,
    String etat,
    String responsableId,
    String ancienResponsableId,
    String acteurId,
    Instant le,
    String titre,
    String echeance
) {
    public static final String TYPE = "pilotage.task-assigned.v1";
}
