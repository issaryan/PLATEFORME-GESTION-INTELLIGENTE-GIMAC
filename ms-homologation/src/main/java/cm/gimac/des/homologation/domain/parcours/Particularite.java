package cm.gimac.des.homologation.domain.parcours;

/**
 * Règle d'exécution particulière d'une opération, en complément du geste (extension du §1 : les rangs 15,
 * 16, C1 et 35 portaient des règles codées en dur que ni le geste ni le jalon ne décrivent).
 */
public enum Particularite {
    /** Clôture : les services du périmètre entrent en essai. */
    ENTREE_ESSAIS("Entrée en essai des services"),
    /** Clôture : les services mesurés à 100 % (ou tout le périmètre sans mesure) sont certifiés. */
    FIN_ESSAIS("Fin des essais et certification des services"),
    /** Preuve : cérémonie de clés à trois porteurs et trois codes de contrôle. */
    CEREMONIE_CLES("Cérémonie de clés en trois composantes"),
    /** Opération suivie dans l'onglet de la mise en service (ouverture d'un service supplémentaire). */
    SERVICE_SUPPLEMENTAIRE("Ouverture d'un service supplémentaire");

    private final String libelle;

    Particularite(String libelle) {
        this.libelle = libelle;
    }

    public String libelle() {
        return libelle;
    }
}
