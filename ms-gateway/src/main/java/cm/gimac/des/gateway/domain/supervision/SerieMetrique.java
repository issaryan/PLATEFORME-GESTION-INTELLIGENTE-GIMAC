package cm.gimac.des.gateway.domain.supervision;

import java.util.List;

/**
 * Série temporelle d'une métrique : nom (service, conteneur, hôte ou « plateforme ») et mesures datées ; une
 * valeur nulle signale une mesure absente (service arrêté, collecte manquée), jamais un zéro.
 */
public record SerieMetrique(String nom, List<Point> points) {
    public SerieMetrique {
        points = List.copyOf(points);
    }

    /** Mesure : instant en millisecondes depuis l'époque, valeur ou null. */
    public record Point(long t, Double v) {}
}
