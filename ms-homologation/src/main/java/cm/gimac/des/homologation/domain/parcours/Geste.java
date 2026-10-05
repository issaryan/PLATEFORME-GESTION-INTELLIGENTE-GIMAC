package cm.gimac.des.homologation.domain.parcours;

/**
 * Geste d'exécution d'une opération du référentiel (docs/lot1/modelisateur-processus.md §1) : il remplace
 * les rangs codés en dur. AFFECTER_RESSOURCES, EVENEMENT_EXTERNE et FICHE_SUIVI se réalisent par leur route
 * propre et jamais par une pièce libre ; AUCUN désigne un point de passage clos dès qu'il devient éligible.
 */
public enum Geste {
    DEPOSER_LIVRABLE("Déposer un livrable"),
    DEPOSER_PREUVE("Déposer une preuve"),
    CONSTATER_COUVERTURE("Constater la couverture du périmètre"),
    AFFECTER_RESSOURCES("Affecter les intervenants"),
    COORDONNEES_TECHNIQUES("Communiquer les coordonnées techniques d'essai"),
    FICHE_SUIVI("Fiche de suivi des services"),
    EVENEMENT_EXTERNE("Événement d'un autre service"),
    AUCUN("Aucun geste : point de passage");

    private final String libelle;

    Geste(String libelle) {
        this.libelle = libelle;
    }

    public String libelle() {
        return libelle;
    }

    /** Geste réalisé par sa route propre : aucune pièce libre n'est admise. */
    public boolean dedie() {
        return this == AFFECTER_RESSOURCES || this == EVENEMENT_EXTERNE || this == FICHE_SUIVI || this == AUCUN;
    }

    /** Action proposée sur une opération ouverte : constat, livrable, sinon preuve (les coordonnées s'admettent aussi en preuve). */
    public String action() {
        return switch (this) {
            case CONSTATER_COUVERTURE -> "CONSTATER_COUVERTURE";
            case DEPOSER_LIVRABLE -> "DEPOSER_LIVRABLE";
            default -> "DEPOSER_PREUVE";
        };
    }
}
