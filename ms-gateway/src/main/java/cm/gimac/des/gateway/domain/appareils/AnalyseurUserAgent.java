package cm.gimac.des.gateway.domain.appareils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Analyse locale de l'en-tête User-Agent (docs/lot1/mon-compte.md §3), sans service tiers : famille
 * et version majeure du navigateur, système et sa version, catégorie d'appareil. Un en-tête absent ou
 * non reconnu donne « Inconnu ». Les chaînes produites sont bornées à 60 caractères.
 */
public final class AnalyseurUserAgent {

    /** Longueur lue de l'en-tête (au-delà, ignoré). */
    static final int LONGUEUR_MAX = 512;
    static final int LIBELLE_MAX = 60;

    private record Regle(String nom, Pattern motif) {}

    /** Ordre significatif : les navigateurs fondés sur Chromium se déclarent aussi « Chrome » et « Safari ». */
    private static final Regle[] NAVIGATEURS = {
        new Regle("Edge", Pattern.compile("(?:Edg|EdgA|EdgiOS|Edge)/(\\d+)")),
        new Regle("Opera", Pattern.compile("(?:OPR|OPiOS)/(\\d+)")),
        new Regle("Samsung Internet", Pattern.compile("SamsungBrowser/(\\d+)")),
        new Regle("Firefox", Pattern.compile("(?:Firefox|FxiOS)/(\\d+)")),
        new Regle("Chrome", Pattern.compile("(?:Chrome|CriOS)/(\\d+)")),
        new Regle("Safari", Pattern.compile("Version/(\\d+)[^ ]* (?:Mobile/\\S+ )?Safari/")),
        new Regle("Internet Explorer", Pattern.compile("(?:MSIE |Trident/.*rv:)(\\d+)")),
    };

    private static final Pattern WINDOWS = Pattern.compile("Windows NT (\\d+\\.\\d+)");
    private static final Pattern ANDROID = Pattern.compile("Android (\\d+)");
    private static final Pattern IOS = Pattern.compile("(?:iPhone|CPU) OS (\\d+)");
    private static final Pattern MACOS = Pattern.compile("Mac OS X (\\d+)[_.](\\d+)");

    private AnalyseurUserAgent() {}

    public static Appareil analyser(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return Appareil.inconnu();
        }
        String ua = userAgent.length() > LONGUEUR_MAX ? userAgent.substring(0, LONGUEUR_MAX) : userAgent;
        return new Appareil(borne(navigateur(ua)), borne(systeme(ua)), type(ua));
    }

    static String navigateur(String ua) {
        for (Regle r : NAVIGATEURS) {
            Matcher m = r.motif().matcher(ua);
            if (m.find()) {
                return r.nom() + " " + m.group(1);
            }
        }
        return Appareil.INCONNU;
    }

    static String systeme(String ua) {
        Matcher m = WINDOWS.matcher(ua);
        if (m.find()) {
            return switch (m.group(1)) {
                case "10.0" -> "Windows 10/11";
                case "6.3" -> "Windows 8.1";
                case "6.2" -> "Windows 8";
                case "6.1" -> "Windows 7";
                default -> "Windows";
            };
        }
        m = ANDROID.matcher(ua);
        if (m.find()) {
            return "Android " + m.group(1);
        }
        m = IOS.matcher(ua);
        if (m.find()) {
            return (ua.contains("iPad") ? "iPadOS " : "iOS ") + m.group(1);
        }
        if (ua.contains("CrOS")) {
            return "ChromeOS";
        }
        m = MACOS.matcher(ua);
        if (m.find()) {
            return "macOS";
        }
        if (ua.contains("Macintosh")) {
            return "macOS";
        }
        if (ua.contains("Linux") || ua.contains("X11")) {
            return "Linux";
        }
        return Appareil.INCONNU;
    }

    static TypeAppareil type(String ua) {
        if (ua.contains("iPad") || ua.contains("Tablet") || (ua.contains("Android") && !ua.contains("Mobile"))) {
            return TypeAppareil.TABLETTE;
        }
        if (ua.contains("Mobi") || ua.contains("iPhone") || ua.contains("iPod")) {
            return TypeAppareil.MOBILE;
        }
        if (ua.contains("Windows NT") || ua.contains("Macintosh") || ua.contains("CrOS") || ua.contains("X11") || ua.contains("Linux")) {
            return TypeAppareil.ORDINATEUR;
        }
        return TypeAppareil.INCONNU;
    }

    private static String borne(String s) {
        return s.length() > LIBELLE_MAX ? s.substring(0, LIBELLE_MAX) : s;
    }
}
