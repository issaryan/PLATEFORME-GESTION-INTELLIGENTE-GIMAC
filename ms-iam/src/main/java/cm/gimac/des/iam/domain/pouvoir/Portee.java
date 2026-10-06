package cm.gimac.des.iam.domain.pouvoir;

/**
 * Portée d'un pouvoir (D-81, docs/lot1/pouvoirs-hierarchiques.md §1), de la plus étroite à la plus large :
 * aucune, subordonnés directs (n+1), équipe (chaîne des responsables), unités (équipe et membres des
 * unités dirigées, sous-unités comprises).
 */
public enum Portee {
    AUCUNE,
    DIRECTS,
    EQUIPE,
    UNITES;

    /** La plus large des deux portées. */
    public Portee max(Portee autre) {
        return autre != null && autre.ordinal() > ordinal() ? autre : this;
    }

    /** Vrai si cette portée couvre au moins l'autre. */
    public boolean couvre(Portee autre) {
        return ordinal() >= autre.ordinal();
    }
}
