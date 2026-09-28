package cm.gimac.des.certification.domain.enumeration;

/**
 * Automate du dossier de mise en service (REC D-08, CC §6.1.8.1).
 */
public enum FileState {
    OUVERT,
    QUITUS_EN_COURS,
    EN_CORRECTION,
    CERTIFIE,
    MISE_EN_SERVICE_AUTORISEE,
}
