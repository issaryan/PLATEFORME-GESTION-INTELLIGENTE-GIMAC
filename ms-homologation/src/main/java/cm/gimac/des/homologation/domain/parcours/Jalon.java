package cm.gimac.des.homologation.domain.parcours;

/**
 * Statut de la liaison atteint à la clôture de l'opération qui le porte (§1). L'ordre de déclaration est
 * celui du parcours : un jalon ne peut précéder un jalon antérieur dans le graphe (contrôle de publication).
 */
public enum Jalon {
    APTE_CERTIFICATION("Apte à la certification"),
    CERTIFIEE("Certifiée"),
    APPROUVEE_SECURITE("Approuvée par la sécurité"),
    EN_PRODUCTION("En production");

    private final String libelle;

    Jalon(String libelle) {
        this.libelle = libelle;
    }

    public String libelle() {
        return libelle;
    }
}
