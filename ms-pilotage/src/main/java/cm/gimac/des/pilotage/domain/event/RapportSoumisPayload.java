package cm.gimac.des.pilotage.domain.event;

import java.time.Instant;
import java.util.UUID;

/** Donnée de pilotage.report-submitted.v1 (sujet pilotage.report, clé = rapport) ; jamais le contenu. */
public record RapportSoumisPayload(UUID rapportId, String type, String periode, String etat, String auteurId, String destinataireId, String acteurId, Instant le) {
    public static final String TYPE = "pilotage.report-submitted.v1";
}
