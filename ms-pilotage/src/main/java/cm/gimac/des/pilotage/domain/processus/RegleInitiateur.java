package cm.gimac.des.pilotage.domain.processus;

/**
 * Règle désignant qui peut lancer une version publiée (D-98 §1), en données dans la définition ; plusieurs règles se
 * cumulent (l'une suffit).
 */
public enum RegleInitiateur {
    /** Membres de l'unité propriétaire, sous-unités comprises (défaut, comportement antérieur). */
    MEMBRES_UNITE,
    /** Tout compte interne actif du GIMAC (demande de congé, demande d'intervention). */
    TOUT_LE_GIMAC,
    /** Membres des unités désignées ({@code unites}), sous-unités comprises. */
    UNITES,
    /** Porteurs de l'un des profils désignés ({@code profils}). */
    PROFILS,
    /** Porteurs de l'une des permissions désignées ({@code permissions}, lues dans le jeton). */
    PERMISSIONS,
}
