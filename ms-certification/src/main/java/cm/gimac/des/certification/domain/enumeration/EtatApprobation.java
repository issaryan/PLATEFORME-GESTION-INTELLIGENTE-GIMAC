package cm.gimac.des.certification.domain.enumeration;

/** États de l'approbation de la sécurité (verrou 2, D-56), déduits des faits enregistrés. */
public enum EtatApprobation {
    EN_ATTENTE_DOSSIER,
    DOSSIER_CONSTITUE,
    EN_VERIFICATION,
    VERIFIE_ARCHIVE,
    APPROUVEE,
    REFUSEE,
}
