package cm.gimac.des.iam.domain.enumeration;

/**
 * Niveaux d'habilitation du CCT (BF-02), du moins au plus privilégié.
 * Décision D-31 (REC-GIMAC-01) : l'échelle du CCT prévaut ; les catégories du
 * CC §6.1.2.1 (administrateur, responsable, opérateur...) deviennent des rôles.
 */
public enum HabilitationLevel {
    CONSULTATION,
    EXECUTION,
    APPROBATION,
    ADMINISTRATION_METIER,
    AUDIT_SECURITE,
}
