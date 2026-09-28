package cm.gimac.des.incident.domain.enumeration;

/**
 * Nature d'une entrée de main courante de crise (écart CC : chaîne libre typée).
 */
public enum CrisisLogNature {
    PRESENCE,
    HYPOTHESE,
    DIRECTIVE,
    DECISION,
    MESURE_CONSERVATOIRE,
    ANOMALIE_CONVOCATION,
    RETOUR_GESTION_COURANTE,
}
