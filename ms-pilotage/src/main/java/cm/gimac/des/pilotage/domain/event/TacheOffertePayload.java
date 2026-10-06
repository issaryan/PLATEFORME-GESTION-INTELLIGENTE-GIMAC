package cm.gimac.des.pilotage.domain.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Donnée de pilotage.task-offered.v1 (sujet pilotage.task, D-81 §6) : tâche confiée à une unité, à prendre ;
 * {@code destinataires} : membres actifs de l'unité et de ses sous-unités (créateur exclu), que
 * MS-NOTIFICATION avertit (type TACHE).
 */
public record TacheOffertePayload(UUID tacheId, String titre, String uniteCible, List<String> destinataires, String acteurId, Instant le) {
    public static final String TYPE = "pilotage.task-offered.v1";

    public TacheOffertePayload {
        destinataires = destinataires == null ? List.of() : List.copyOf(destinataires);
    }
}
