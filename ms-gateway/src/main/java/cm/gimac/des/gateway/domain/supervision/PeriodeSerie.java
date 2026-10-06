package cm.gimac.des.gateway.domain.supervision;

import java.time.Duration;

/**
 * Périodes d'historique proposées : liste fermée, pas calculé pour environ 240 points par série (lisible,
 * léger pour Prometheus et le navigateur), jamais sous l'intervalle de collecte de 15 s.
 */
public enum PeriodeSerie {
    UNE_HEURE(Duration.ofHours(1)),
    SIX_HEURES(Duration.ofHours(6)),
    UN_JOUR(Duration.ofDays(1)),
    SEPT_JOURS(Duration.ofDays(7));

    static final int POINTS = 240;
    static final Duration PAS_MIN = Duration.ofSeconds(15);

    private final Duration duree;

    PeriodeSerie(Duration duree) {
        this.duree = duree;
    }

    public Duration duree() {
        return duree;
    }

    /** Pas des mesures, arrondi à la seconde. */
    public Duration pas() {
        Duration brut = Duration.ofSeconds(Math.max(1, duree.toSeconds() / POINTS));
        return brut.compareTo(PAS_MIN) < 0 ? PAS_MIN : brut;
    }
}
