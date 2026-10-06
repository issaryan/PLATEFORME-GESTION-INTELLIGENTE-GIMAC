package cm.gimac.des.pilotage.domain.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Donnée de pilotage.process-instance-closed.v1 (sujet pilotage.process, clé = instance, D-87) : instance close
 * (toutes étapes terminées, {@code etat} CLOSE) ou annulée par le chef de l'unité ({@code etat} ANNULEE, motif) ;
 * {@code destinataires} : le lanceur (s'il n'est pas l'acteur).
 */
public record InstanceClosePayload(
    UUID instanceId,
    UUID processusId,
    String code,
    String intitule,
    String etat,
    String lanceurId,
    String acteurId,
    String motif,
    List<String> destinataires,
    String lien,
    Instant le
) {
    public static final String TYPE = "pilotage.process-instance-closed.v1";

    public InstanceClosePayload {
        destinataires = destinataires == null ? List.of() : List.copyOf(destinataires);
    }
}
