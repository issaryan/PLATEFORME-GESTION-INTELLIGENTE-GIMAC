package cm.gimac.des.gateway.domain.supervision;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Masquage des secrets de configuration (D-59 « aucun secret », BNF-07). Appliqué par la
 * passerelle en plus de l'assainissement de Spring (/management/env) : une propriété dont le
 * nom désigne un secret, ou dont la valeur en a l'allure (jeton JWT, clé PEM, mot de passe
 * dans une adresse), n'est jamais restituée en clair.
 */
public final class MasqueSecrets {

    public static final String MASQUE = "******";

    /** Fragments de nom qui désignent un secret, où qu'ils apparaissent dans la clé. */
    private static final Set<String> FRAGMENTS = Set.of(
        "password",
        "passwd",
        "passphrase",
        "secret",
        "secrete",
        "token",
        "jeton",
        "credential",
        "private",
        "privee",
        "apikey",
        "api-key",
        "api_key",
        "motdepasse",
        "mot-de-passe",
        "signature",
        "sas",
        "vault"
    );

    /** Segments de nom (entre points, tirets ou soulignés) qui désignent un secret à eux seuls. */
    private static final Set<String> SEGMENTS = Set.of("key", "pwd", "pass", "cle", "mdp", "pin", "otp", "salt", "keystore", "truststore");

    private static final Pattern JWT = Pattern.compile("eyJ[A-Za-z0-9_-]{5,}\\.[A-Za-z0-9_-]{5,}\\.[A-Za-z0-9_-]*");
    private static final Pattern PEM = Pattern.compile("-----BEGIN [A-Z ]+-----");
    /** Mot de passe dans l'autorité d'une adresse : schéma://utilisateur:motdepasse@hôte. */
    private static final Pattern AUTORITE = Pattern.compile("(?<debut>[a-zA-Z][a-zA-Z0-9+.-]*://[^:/@\\s]*:)(?<secret>[^@/\\s]+)(?<fin>@)");
    /** Paramètre sensible dans une chaîne de requête ou une chaîne de connexion. */
    private static final Pattern PARAMETRE = Pattern.compile(
        "(?i)(?<debut>(?:password|passwd|pwd|secret|token|apikey|api_key|access_token|sslpassword)=)(?<secret>[^&;\\s]+)"
    );

    private MasqueSecrets() {}

    /** Le nom de propriété désigne-t-il un secret ? */
    public static boolean cleSensible(String cle) {
        if (cle == null) {
            return false;
        }
        String c = cle.toLowerCase(Locale.ROOT);
        if (FRAGMENTS.stream().anyMatch(f -> contientFragment(c, f))) {
            return true;
        }
        return Arrays.stream(c.split("[.\\-_\\[\\]]")).anyMatch(SEGMENTS::contains);
    }

    /** « sas » ne vaut que comme segment entier, pour ne pas masquer « sasl » ni « kafka.sasl.mechanism ». */
    private static boolean contientFragment(String cle, String fragment) {
        if (!"sas".equals(fragment)) {
            return cle.contains(fragment);
        }
        return Arrays.asList(cle.split("[.\\-_]")).contains("sas");
    }

    /** Valeur à restituer pour la propriété : masquée entièrement ou dans ses parties sensibles. */
    public static String masquer(String cle, String valeur) {
        if (valeur == null) {
            return null;
        }
        if (cleSensible(cle) && !valeur.isEmpty()) {
            return MASQUE;
        }
        return masquerValeur(valeur);
    }

    /** Masque, dans une valeur quelconque, ce qui a l'allure d'un secret. */
    public static String masquerValeur(String valeur) {
        if (valeur == null) {
            return null;
        }
        if (JWT.matcher(valeur).find() || PEM.matcher(valeur).find()) {
            return MASQUE;
        }
        String v = AUTORITE.matcher(valeur).replaceAll("${debut}" + MASQUE + "${fin}");
        return PARAMETRE.matcher(v).replaceAll("${debut}" + MASQUE);
    }
}
