package cm.gimac.des.iam.domain.compte;

import java.util.Locale;
import java.util.Optional;

/**
 * Actions exigées d'un compte par la plateforme (D-65), conduites dans l'application de sa couche et
 * tenues par MS-IAM dans l'attribut {@code gimac_actions_requises}, jamais par les actions requises de
 * Keycloak (qui bloqueraient l'octroi direct). Un compte créé par l'administration reçoit les quatre
 * premières (premier accès).
 */
public enum ActionRequise {
    MOT_DE_PASSE,
    SECOND_FACTEUR,
    FICHE,
    CHARTE,
    VERIFIER_COURRIEL;

    /**
     * Lit une action, y compris sous son ancien nom (actions requises de Keycloak : UPDATE_PASSWORD,
     * CONFIGURE_TOTP, UPDATE_PROFILE, VERIFY_EMAIL) ; vide pour une valeur inconnue.
     */
    public static Optional<ActionRequise> lire(String valeur) {
        if (valeur == null) {
            return Optional.empty();
        }
        return switch (valeur.trim().toUpperCase(Locale.ROOT)) {
            case "MOT_DE_PASSE", "UPDATE_PASSWORD" -> Optional.of(MOT_DE_PASSE);
            case "SECOND_FACTEUR", "CONFIGURE_TOTP" -> Optional.of(SECOND_FACTEUR);
            case "FICHE", "UPDATE_PROFILE" -> Optional.of(FICHE);
            case "CHARTE" -> Optional.of(CHARTE);
            case "VERIFIER_COURRIEL", "VERIFY_EMAIL" -> Optional.of(VERIFIER_COURRIEL);
            default -> Optional.empty();
        };
    }
}
