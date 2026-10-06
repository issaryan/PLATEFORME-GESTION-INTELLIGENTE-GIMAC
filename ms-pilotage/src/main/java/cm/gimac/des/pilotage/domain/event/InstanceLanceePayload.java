package cm.gimac.des.pilotage.domain.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Donnée de pilotage.process-instance-started.v1 (sujet pilotage.process, clé = instance, D-87) : instance lancée
 * ({@code destinataires} : chef de l'unité, s'il n'est pas le lanceur).
 */
public record InstanceLanceePayload(
    UUID instanceId,
    UUID processusId,
    String code,
    int version,
    String unite,
    String intitule,
    String lanceurId,
    List<String> destinataires,
    String lien,
    Instant le
) {
    public static final String TYPE = "pilotage.process-instance-started.v1";

    public InstanceLanceePayload {
        destinataires = destinataires == null ? List.of() : List.copyOf(destinataires);
    }
}
