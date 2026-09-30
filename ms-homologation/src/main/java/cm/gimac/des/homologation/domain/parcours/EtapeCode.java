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
}
