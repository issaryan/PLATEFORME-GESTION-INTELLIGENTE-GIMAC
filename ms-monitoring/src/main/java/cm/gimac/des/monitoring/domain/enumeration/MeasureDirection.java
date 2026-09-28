package cm.gimac.des.monitoring.domain.enumeration;

/**
 * Sens défavorable d'un indicateur : d = +1 (latence) ou d = -1 (taux de succès).
 */
public enum MeasureDirection {
    HAUSSE_DEFAVORABLE,
    BAISSE_DEFAVORABLE,
}
