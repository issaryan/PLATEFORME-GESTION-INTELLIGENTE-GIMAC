package cm.gimac.des.homologation.domain.parcours;

/** Plateforme d'exécution d'une opération (CR p.38, p.54 ; D-59). */
public enum PlateformeExecution {
    SWITCH_INTERBANCAIRE,
    SWITCH_DELEGATAIRE,
    PLATEFORME_MOBILE,
    SIMULATEUR;

    /** Libellé français de la plateforme (catalogue du modélisateur). */
    public String libelle() {
        return switch (this) {
            case SWITCH_INTERBANCAIRE -> "Switch interbancaire";
            case SWITCH_DELEGATAIRE -> "Switch délégataire";
            case PLATEFORME_MOBILE -> "Plateforme de paiement mobile";
            case SIMULATEUR -> "Simulateur";
        };
    }
}
