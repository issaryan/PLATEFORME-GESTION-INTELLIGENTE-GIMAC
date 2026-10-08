package cm.gimac.des.pilotage.domain.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Donnée de pilotage.report-submitted.v1 (sujet pilotage.report, clé = rapport) ; jamais le contenu. {@code etat} SOUMIS,
 * ou DEPOSE sans destinataire ; {@code destinataireMotif} : N_PLUS_UN, CHEF_UNITE, CHEF_UNITE_PARENTE, CHOISI, ou nul.
 */
public record RapportSoumisPayload(
    UUID rapportId,
    String type,
    String periode,
    String etat,
    String auteurId,
    String destinataireId,
    String acteurId,
    Instant le,
    String destinataireMotif
) {
    public static final String TYPE = "pilotage.report-submitted.v1";
}
