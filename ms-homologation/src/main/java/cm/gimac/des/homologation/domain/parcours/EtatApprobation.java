package cm.gimac.des.homologation.domain.parcours;

/** Projection de l'état de l'approbation de la sécurité tenue par MS-CERTIFICATION (verrou 2, D-56). */
public enum EtatApprobation {
    EN_ATTENTE_DOSSIER,
    DOSSIER_CONSTITUE,
    EN_VERIFICATION,
    VERIFIE_ARCHIVE,
    APPROUVEE,
    REFUSEE,
}
