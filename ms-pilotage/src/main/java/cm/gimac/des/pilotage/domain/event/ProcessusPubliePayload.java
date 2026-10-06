package cm.gimac.des.pilotage.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Donnée de pilotage.process-published.v1 (sujet pilotage.process, clé = version publiée, D-87) : version
 * publiée d'un processus d'unité ; {@code versionRetiree} : version publiée précédente, retirée du même coup.
 */
public record ProcessusPubliePayload(
    UUID processusId,
    String code,
    int version,
    String unite,
    String libelle,
    String acteurId,
    String motif,
    boolean avertissementsLeves,
    UUID versionRetiree,
    Instant le
) {
    public static final String TYPE = "pilotage.process-published.v1";
}
