package cm.gimac.des.homologation.domain.enumeration;

/**
 * The Segment enumeration.
 */
public enum Segment {
    TRONC_COMMUN_AMONT,
    BRANCHE,
    TRONC_COMMUN_AVAL;

    /**
     * Lecture d'une saisie : les valeurs renommées du modélisateur (AMONT, BRANCHE, AVAL, D-77) et les
     * valeurs historiques sont admises.
     */
    @com.fasterxml.jackson.annotation.JsonCreator
    public static Segment de(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        return switch (valeur.trim()) {
            case "AMONT", "TRONC_COMMUN_AMONT" -> TRONC_COMMUN_AMONT;
            case "BRANCHE" -> BRANCHE;
            case "AVAL", "TRONC_COMMUN_AVAL" -> TRONC_COMMUN_AVAL;
            default -> throw new IllegalArgumentException("Segment inconnu : " + valeur);
        };
    }

    /** Valeur renommée servie par les vues du modélisateur et utilisée par le parcours. */
    public cm.gimac.des.homologation.domain.parcours.SegmentParcours parcours() {
        return switch (this) {
            case TRONC_COMMUN_AMONT -> cm.gimac.des.homologation.domain.parcours.SegmentParcours.AMONT;
            case BRANCHE -> cm.gimac.des.homologation.domain.parcours.SegmentParcours.BRANCHE;
            case TRONC_COMMUN_AVAL -> cm.gimac.des.homologation.domain.parcours.SegmentParcours.AVAL;
        };
    }

    public static Segment du(cm.gimac.des.homologation.domain.parcours.SegmentParcours s) {
        return switch (s) {
            case AMONT -> TRONC_COMMUN_AMONT;
            case BRANCHE -> BRANCHE;
            case AVAL -> TRONC_COMMUN_AVAL;
        };
    }
}
