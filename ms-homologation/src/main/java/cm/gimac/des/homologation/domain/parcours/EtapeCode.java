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

    /**
     * Étape canonique porteuse d'un jalon (apte à la certification et certifiée, en production) ou du verrou 2
     * (D-89) : une étape configurée qui lui est liée ne se supprime pas et ne change pas de canonique.
     */
    public boolean protegee() {
        return this == CERTIFICATION || this == APPROBATION_SECURITE || this == MISE_EN_PRODUCTION;
    }

    /** Couleur par défaut de l'étape dans le modélisateur (version 1 et nouveaux brouillons, D-89). */
    public CouleurEtape couleur() {
        return switch (this) {
            case PRISE_CONTACT, ADHESION, SUIVI -> CouleurEtape.NEUTRE;
            case CADRAGE, LANCEMENT -> CouleurEtape.INFO;
            case ATELIERS, INTEGRATION -> CouleurEtape.ACTION;
            case TESTS -> CouleurEtape.ALERTE;
            case CERTIFICATION, MISE_EN_PRODUCTION -> CouleurEtape.SUCCES;
            case APPROBATION_SECURITE -> CouleurEtape.CRITIQUE;
        };
    }

    /** Étape canonique de ce code, le cas échéant (code d'une étape configurée, phase historique). */
    public static java.util.Optional<EtapeCode> de(String code) {
        if (code == null) {
            return java.util.Optional.empty();
        }
        for (EtapeCode e : values()) {
            if (e.name().equals(code.trim())) {
                return java.util.Optional.of(e);
            }
        }
        return java.util.Optional.empty();
    }
}
