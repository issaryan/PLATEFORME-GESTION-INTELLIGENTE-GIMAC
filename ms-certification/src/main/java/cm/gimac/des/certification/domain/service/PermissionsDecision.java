package cm.gimac.des.certification.domain.service;

/**
 * Permissions de la matrice des rôles pour les verrous de la décision (frontend/packages/lot1/src/permissions.ts,
 * docs/lot1/correspondance.md) : elles gardent les routes (@PreAuthorize) et filtrent les actions permises.
 */
public final class PermissionsDecision {

    /** Lecture du procès-verbal et de l'approbation (tous les profils du DES). */
    public static final String LIRE = "CER_DOSSIER_READ";
    /** Établissement du procès-verbal (conducteur de projet, rang 18). */
    public static final String ETABLIR_PV = "CER_PV_ESTABLISH";
    /** Signature GIMAC du procès-verbal (chef du DES : ancien quitus technique). */
    public static final String SIGNER_PV_GIMAC = "CER_QUITUS_TECHNIQUE_SIGN";
    /** Dossier et fiche de changement (production monétique). */
    public static final String LIER_CHANGEMENT = "CER_CHANGE_RECORD_LINK";
    /** Vérification et archivage des attestations (sécurité des SI, rang 23). */
    public static final String VERIFIER_SECURITE = "CER_SECURITY_VERIFY";
    /** Visa optionnel de la conformité. */
    public static final String VISA_CONFORMITE = "CER_QUITUS_CONFORMITE_SIGN";
    /** Visa optionnel de la Direction Générale. */
    public static final String VISA_DG = "CER_QUITUS_DG_SIGN";
    /** Portail : lecture. */
    public static final String PORTAIL_LIRE = "CER_PORTAL_READ";
    /** Portail : signature du procès-verbal par le participant (rang 19). */
    public static final String PORTAIL_SIGNER = "CER_PORTAL_SIGN";
    /** Portail : dépôt des attestations (rangs 21 et 22). */
    public static final String PORTAIL_DEPOSER = "HOM_PORTAL_SUBMIT";

    private PermissionsDecision() {}
}
