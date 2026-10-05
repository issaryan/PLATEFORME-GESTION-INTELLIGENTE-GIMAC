package cm.gimac.des.pilotage.domain.event;

import java.time.Instant;
import java.util.UUID;

/** Donnée de pilotage.report-visaed.v1 (sujet pilotage.report, clé = rapport) ; jamais le contenu ni le commentaire. */
public record RapportVisePayload(UUID rapportId, String type, String periode, String etat, String auteurId, String destinataireId, String acteurId, Instant le) {
    public static final String TYPE = "pilotage.report-visaed.v1";
}
