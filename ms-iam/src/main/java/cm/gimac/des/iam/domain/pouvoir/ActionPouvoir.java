package cm.gimac.des.iam.domain.pouvoir;

/**
 * Actions de la matrice des pouvoirs (D-81, docs/lot1/pouvoirs-hierarchiques.md §3 ; D-86,
 * docs/lot1/profils-organisation.md §4 pour les processus d'unité).
 */
public enum ActionPouvoir {
    /** Confier une tâche à un subordonné ou à une unité dirigée, la réaffecter. */
    TACHE_CONFIER,
    /** Voir les tâches des subordonnés et les relancer. */
    TACHE_SUIVRE,
    /** Lire agenda et activités du jour. */
    ACTIVITE_LIRE,
    /** Lire et viser les rapports. */
    RAPPORT_VISER,
    /** Changer l'unité (dans ses unités dirigées) ou le n+1 (dans son équipe) d'un subordonné. */
    RATTACHEMENT_MODIFIER,
    /** Demander à l'administration la création d'un compte de subordonné. */
    COMPTE_DEMANDER,
    /** Demander l'ajout ou le retrait d'un profil pour un subordonné. */
    PROFIL_DEMANDER,
    /** Demander la désactivation ou la réactivation d'un subordonné. */
    STATUT_DEMANDER,
    /** Créer et publier les processus des unités couvertes (D-86 §4, D-87). */
    PROCESSUS_DEFINIR,
    /** Lancer un processus d'une unité couverte (D-86 §4, D-87) ; un membre lance ceux de son unité sans pouvoir. */
    PROCESSUS_LANCER,
}
