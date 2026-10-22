package cm.gimac.des.gateway.domain.sandbox;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Route d'un écran de la sandbox : couche de présentation (portail, console ou département) et service de la sandbox
 * visé. Les onze services et leur module de la matrice SBX_&lt;MODULE&gt;_&lt;FONCTION&gt; (US-EXI-02) sont fixés ici.
 */
public record RouteSandbox(String couche, String service) {

    /** Service de la sandbox (sans préfixe sbx-) vers son module de la matrice des permissions. */
    public static final Map<String, String> MODULES = Map.ofEntries(
        Map.entry("espace", "HAB"),
        Map.entry("raccordement", "RAC"),
        Map.entry("referentiel", "REF"),
        Map.entry("donnees", "DON"),
        Map.entry("simulation", "SIM"),
        Map.entry("mobile", "MOB"),
        Map.entry("iso20022", "ISO"),
        Map.entry("carte", "CAR"),
        Map.entry("campagne", "CER"),
        Map.entry("trace", "TRA"),
        Map.entry("lecture", "LEC")
    );

    private static final Pattern CHEMIN = Pattern.compile("^/bff/(portail|console|des)/sbx-([a-z0-9]+)(/.*)?$");

    /** Route de la sandbox désignée par ce chemin d'origine, vide si le chemin n'en désigne aucune. */
    public static Optional<RouteSandbox> lire(String chemin) {
        if (chemin == null) {
            return Optional.empty();
        }
        Matcher m = CHEMIN.matcher(chemin);
        if (!m.matches() || !MODULES.containsKey(m.group(2))) {
            return Optional.empty();
        }
        return Optional.of(new RouteSandbox(m.group(1), m.group(2)));
    }

    /** Nom du service tel que le connaissent la configuration et les jetons (sbx-espace…). */
    public String nomService() {
        return "sbx-" + service;
    }

    public String module() {
        return MODULES.get(service);
    }
}
