package cm.gimac.des.homologation.domain.parcours;

/**
 * Les étapes du parcours d'une liaison (CR-CERT-GIMAC-01, D-57) ; APPROBATION_SECURITE n'est pas une
 * étape mais le verrou 2 (D-56), porté comme étape courante entre la certification et la mise en production.
 */
public enum EtapeCode {
    PRISE_CONTACT(1),
    ADHESION(2),
    CADRAGE(3),
    LANCEMENT(4),
    ATELIERS(5),
    INTEGRATION(6),
    TESTS(7),
    CERTIFICATION(8),
    APPROBATION_SECURITE(null),
    MISE_EN_PRODUCTION(9),
    SUIVI(10);

    private final Integer rang;

    EtapeCode(Integer rang) {
        this.rang = rang;
    }

    /** Numéro de l'étape dans le CR ; null pour le verrou 2. */
    public Integer rang() {
        return rang;
    }

    /** Libellé français de l'étape (catalogue du modélisateur). */
    public String libelle() {
        return switch (this) {
            case PRISE_CONTACT -> "Prise de contact";
            case ADHESION -> "Adhésion";
            case CADRAGE -> "Cadrage";
            case LANCEMENT -> "Lancement";
            case ATELIERS -> "Ateliers";
            case INTEGRATION -> "Intégration";
            case TESTS -> "Tests";
            case CERTIFICATION -> "Certification";
            case APPROBATION_SECURITE -> "Approbation de la sécurité";
            case MISE_EN_PRODUCTION -> "Mise en production";
            case SUIVI -> "Suivi post-production";
        };
    }

    /** Étapes 1 et 2, tenues par MS-PARTICIPANT et closes à l'ouverture de la liaison : jamais instanciées. */
    public boolean horsParcours() {
        return this == PRISE_CONTACT || this == ADHESION;
    }
}
