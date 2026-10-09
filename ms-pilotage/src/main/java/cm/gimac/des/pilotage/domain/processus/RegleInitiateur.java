package cm.gimac.des.pilotage.domain.processus;

/**
 * Règle désignant qui peut voir et lancer une version publiée (D-98 §1, D-100), en données dans la définition ; plusieurs
 * règles se cumulent (l'une suffit). D-100 : ces règles, choisies par le créateur du processus, font seules foi pour le
 * catalogue « Lancer une demande » et le lancement ; aucun passe-droit global (REF_METIER_ADMIN, PROCESSUS_LANCER) ne
 * s'y ajoute.
 */
public enum RegleInitiateur {
    /**
     * Membres de l'unité propriétaire, sous-unités comprises, et ses responsables (chef qui dirige l'unité ou l'une de
     * ses unités parentes, D-100) ; défaut.
     */
    MEMBRES_UNITE,
    /** Tout compte interne actif du GIMAC (demande de congé, demande d'intervention). */
    TOUT_LE_GIMAC,
    /** Membres des unités désignées ({@code unites}), sous-unités comprises. */
    UNITES,
    /** Porteurs de l'un des profils désignés ({@code profils}). */
    PROFILS,
    /** Porteurs de l'une des permissions désignées ({@code permissions}, lues dans le jeton). */
    PERMISSIONS,
    /** Personnes désignées nommément ({@code personnes}, identifiants de comptes), comptes internes actifs (D-100). */
    PERSONNES,
}
