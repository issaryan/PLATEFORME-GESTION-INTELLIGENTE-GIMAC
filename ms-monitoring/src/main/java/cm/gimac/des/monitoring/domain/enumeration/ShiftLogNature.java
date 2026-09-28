package cm.gimac.des.monitoring.domain.enumeration;

/**
 * Nature d'une entrée de main courante de quart (écart CC : chaîne libre typée).
 */
public enum ShiftLogNature {
    EVENEMENT,
    ALERTE,
    ACQUITTEMENT_HORS_DELAI,
    INTERVENTION,
    RETABLISSEMENT,
    MAINTENANCE,
    CONSIGNE,
    RESERVE_ENTRANT,
    CORRECTION,
}
