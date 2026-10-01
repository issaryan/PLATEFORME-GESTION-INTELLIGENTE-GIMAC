package cm.gimac.des.gateway.domain.appareils;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Appareil d'une session ou d'une tentative de connexion : navigateur et système (avec leur version
 * majeure, pour l'affichage) et catégorie.
 */
public record Appareil(String navigateur, String systeme, TypeAppareil type) {
    public static final String INCONNU = "Inconnu";
    private static final Pattern VERSION = Pattern.compile("\\s+[0-9][0-9.]*$");

    public Appareil {
        navigateur = navigateur == null || navigateur.isBlank() ? INCONNU : navigateur;
        systeme = systeme == null || systeme.isBlank() ? INCONNU : systeme;
        type = type == null ? TypeAppareil.INCONNU : type;
    }

    public static Appareil inconnu() {
        return new Appareil(INCONNU, INCONNU, TypeAppareil.INCONNU);
    }

    /**
     * Identité de l'appareil pour la détection d'un nouvel appareil : familles du navigateur et du
     * système, sans les versions (une mise à jour du navigateur n'est pas un nouvel appareil).
     */
    public String empreinte() {
        return famille(navigateur) + "|" + famille(systeme);
    }

    static String famille(String nom) {
        return VERSION.matcher(nom.trim()).replaceAll("").toLowerCase(Locale.ROOT);
    }
}
