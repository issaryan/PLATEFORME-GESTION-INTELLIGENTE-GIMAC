package cm.gimac.des.iam.domain.compte;

import java.util.Locale;

/**
 * Couche de présentation d'où part un parcours (D-65) : application du département, portail des
 * participants, console d'administration. Le lien d'un courriel mène à la couche du compte.
 */
public enum Couche {
    DES,
    PORTAIL,
    CONSOLE;

    /** Couche nommée (« des », « portail », « console ») ; DES à défaut. */
    public static Couche lire(String valeur) {
        if (valeur == null) {
            return DES;
        }
        try {
            return valueOf(valeur.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return DES;
        }
    }

    /**
     * Couche où le compte se connecte : un correspondant (institution) sur le portail seulement ; un
     * compte interne sur le département ou la console, celle demandée si elle lui est ouverte.
     */
    public static Couche pour(Compte compte, Couche demandee) {
        if (compte.institutionId() != null && !compte.institutionId().isBlank()) {
            return PORTAIL;
        }
        return demandee == PORTAIL ? DES : demandee;
    }
}
