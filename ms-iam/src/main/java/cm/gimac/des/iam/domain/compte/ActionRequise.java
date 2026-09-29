package cm.gimac.des.iam.domain.compte;

/**
 * Actions que le fournisseur d'identité impose à la prochaine connexion (actions requises de
 * Keycloak). Un compte créé par l'administration reçoit les quatre premières (premier accès).
 */
public enum ActionRequise {
    UPDATE_PASSWORD,
    CONFIGURE_TOTP,
    UPDATE_PROFILE,
    VERIFY_EMAIL,
}
