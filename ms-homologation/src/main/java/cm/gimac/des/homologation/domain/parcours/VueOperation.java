package cm.gimac.des.homologation.domain.parcours;

import java.util.Locale;

/**
 * Écran spécialisé d'une opération (D-93 §5, D-94 §3) : attribut de l'opération du référentiel, lu par l'interface
 * à la place du rang. {@code AUCUNE} : le panneau générique de l'opération (livrables, preuve, validation, signatures).
 */
public enum VueOperation {
    AUCUNE("Panneau générique de l'opération"),
    AFFECTATION("Affectation des intervenants"),
    COORDONNEES("Coordonnées techniques d'essai"),
    ESSAIS("Essais et couverture"),
    DECISION("Décision : procès-verbal et approbation de la sécurité"),
    ATTESTATIONS("Attestations du participant"),
    MISE_EN_SERVICE("Mise en service et fiche de suivi"),
    CEREMONIE("Cérémonie de clés");

    private final String libelle;

    VueOperation(String libelle) {
        this.libelle = libelle;
    }

    public String libelle() {
        return libelle;
    }

    /**
     * Vue déduite du modèle de l'opération quand elle n'est pas saisie : geste, particularité, puis déclencheur ;
     * sinon le panneau générique.
     */
    public static VueOperation deduite(Geste geste, Particularite particularite, String declencheur) {
        if (geste == Geste.AFFECTER_RESSOURCES) {
            return AFFECTATION;
        }
        if (geste == Geste.COORDONNEES_TECHNIQUES) {
            return COORDONNEES;
        }
        if (geste == Geste.CONSTATER_COUVERTURE || particularite == Particularite.ENTREE_ESSAIS || particularite == Particularite.FIN_ESSAIS) {
            return ESSAIS;
        }
        if (geste == Geste.FICHE_SUIVI || particularite == Particularite.SERVICE_SUPPLEMENTAIRE) {
            return MISE_EN_SERVICE;
        }
        if (particularite == Particularite.CEREMONIE_CLES) {
            return CEREMONIE;
        }
        if (Declencheurs.ATTESTATIONS_ARCHIVEES.equals(declencheur)) {
            return ATTESTATIONS;
        }
        return declencheur != null && !declencheur.isBlank() ? DECISION : AUCUNE;
    }

    /**
     * Vue des opérations de la version 1 (annexe A du CR), par rang : c'est la correspondance que l'interface codait
     * en dur ; elle sert la conversion de l'annexe A et la reprise des versions existantes (changelog D-93).
     */
    public static VueOperation duRang(String rang) {
        if (rang == null) {
            return AUCUNE;
        }
        return switch (rang.trim().toUpperCase(Locale.ROOT)) {
            case "6" -> AFFECTATION;
            case "12" -> COORDONNEES;
            case "15", "16", "17" -> ESSAIS;
            case "18", "19", "20", "23", "24", "25", "26" -> DECISION;
            case "21", "22" -> ATTESTATIONS;
            case "32", "33", "34", "35" -> MISE_EN_SERVICE;
            case "C1" -> CEREMONIE;
            default -> AUCUNE;
        };
    }
}
