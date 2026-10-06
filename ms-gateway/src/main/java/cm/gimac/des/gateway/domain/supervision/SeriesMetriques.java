package cm.gimac.des.gateway.domain.supervision;

import java.time.Instant;
import java.util.List;

/**
 * Réponse de /api/plateforme/series : métrique, unité, période et pas, séries triées de la plus chargée à
 * la moins chargée (les premières portent les couleurs d'identité, les suivantes sont regroupées). La
 * disponibilité de Prometheus est signalée sans erreur : la page affiche « mesures indisponibles ».
 */
public record SeriesMetriques(
    MetriqueSerie metrique,
    String unite,
    PeriodeSerie periode,
    Instant debut,
    Instant fin,
    long pasSecondes,
    boolean disponible,
    List<SerieMetrique> series
) {
    public SeriesMetriques {
        series = List.copyOf(series);
    }
}
