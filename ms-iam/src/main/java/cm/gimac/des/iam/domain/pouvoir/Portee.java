package cm.gimac.des.iam.domain.pouvoir;

/**
 * Portée d'un pouvoir (D-81, docs/lot1/pouvoirs-hierarchiques.md §1), de la plus étroite à la plus large :
 * aucune, subordonnés directs (n+1), équipe (chaîne des responsables), unités (équipe et membres des
 * unités dirigées, sous-unités comprises), toutes (D-92, docs/lot1/referentiels-metier-admin.md §2 bis :
 * toutes les unités, actives ou non, admise seulement pour les actions des processus d'unité, voir
 * {@link ActionPouvoir#admet(Portee)}).
 */
public enum Portee {
    AUCUNE,
    DIRECTS,
    EQUIPE,
    UNITES,
    TOUTES;

    /** La plus large des deux portées. */
    public Portee max(Portee autre) {
        return autre != null && autre.ordinal() > ordinal() ? autre : this;
    }

    /** La plus étroite des deux portées. */
    public Portee min(Portee autre) {
        return autre != null && autre.ordinal() < ordinal() ? autre : this;
    }

    /** Vrai si cette portée couvre au moins l'autre. */
    public boolean couvre(Portee autre) {
        return ordinal() >= autre.ordinal();
    }
}
