package cm.gimac.des.iam.domain.service;

import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.domain.compte.PolitiqueSecurite;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Conversion entre la politique de mots de passe de la plateforme et la syntaxe du fournisseur
 * d'identité (« length(12) and upperCase(1) and … »), et contrôle de cohérence (D-62). Service de
 * domaine pur.
 */
public final class PolitiqueMotDePasse {

    /** Planchers imposés par la sécurité des SI : aucune politique plus faible n'est acceptée. */
    public static final int LONGUEUR_PLANCHER = 12;

    private static final Pattern REGLE = Pattern.compile("([a-zA-Z]+)\\(([^)]*)\\)");

    private PolitiqueMotDePasse() {}

    public static String versChaine(PolitiqueSecurite p) {
        List<String> r = new ArrayList<>();
        r.add("length(" + p.longueurMinimale() + ")");
        ajouter(r, "upperCase", p.majusculesMin());
        ajouter(r, "lowerCase", p.minusculesMin());
        ajouter(r, "digits", p.chiffresMin());
        ajouter(r, "specialChars", p.speciauxMin());
        ajouter(r, "passwordHistory", p.historique());
        ajouter(r, "forceExpiredPasswordChange", p.expirationJours());
        if (p.interditIdentifiant()) {
            r.add("notUsername(undefined)");
        }
        return String.join(" and ", r);
    }

    /** Lit une chaîne de politique ; les règles absentes valent 0 (ou faux). */
    public static Map<String, Integer> lire(String chaine) {
        Map<String, Integer> regles = new LinkedHashMap<>();
        if (chaine == null) {
            return regles;
        }
        Matcher m = REGLE.matcher(chaine);
        while (m.find()) {
            regles.put(m.group(1), m.group(2).matches("\\d+") ? Integer.parseInt(m.group(2)) : 1);
        }
        return regles;
    }

    public static void verifier(PolitiqueSecurite p) {
        Map<String, Object> erreurs = new LinkedHashMap<>();
        if (p.longueurMinimale() < LONGUEUR_PLANCHER || p.longueurMinimale() > 128) erreurs.put("longueurMinimale", "12 à 128");
        if (p.majusculesMin() + p.minusculesMin() + p.chiffresMin() + p.speciauxMin() > p.longueurMinimale()) erreurs.put("classes", "somme > longueur");
        if (p.historique() < 0 || p.historique() > 24) erreurs.put("historique", "0 à 24");
        if (p.expirationJours() < 0 || p.expirationJours() > 365) erreurs.put("expirationJours", "0 à 365");
        if (p.otpChiffres() != 6 && p.otpChiffres() != 8) erreurs.put("otpChiffres", "6 ou 8");
        if (p.otpPeriodeSecondes() < 30 || p.otpPeriodeSecondes() > 60) erreurs.put("otpPeriodeSecondes", "30 à 60");
        if (!List.of("HmacSHA1", "HmacSHA256", "HmacSHA512").contains(p.otpAlgorithme())) erreurs.put("otpAlgorithme", "HmacSHA1, HmacSHA256 ou HmacSHA512");
        if (p.sessionInactiviteMinutes() < 5 || p.sessionInactiviteMinutes() > 120) erreurs.put("sessionInactiviteMinutes", "5 à 120");
        if (p.sessionDureeMaxHeures() < 1 || p.sessionDureeMaxHeures() > 24) erreurs.put("sessionDureeMaxHeures", "1 à 24");
        if (!p.forceBruteActive()) erreurs.put("forceBruteActive", "obligatoire");
        if (p.forceBruteEchecsMax() < 3 || p.forceBruteEchecsMax() > 10) erreurs.put("forceBruteEchecsMax", "3 à 10");
        if (p.forceBruteAttenteMinutes() < 1 || p.forceBruteAttenteMinutes() > 1440) erreurs.put("forceBruteAttenteMinutes", "1 à 1440");
        if (!erreurs.isEmpty()) {
            throw IamException.invalide("IAM_POLICY_INVALID", "Politique de sécurité refusée", Map.of("champs", erreurs));
        }
    }

    /**
     * Violations d'un mot de passe au regard de la politique (codes de la plateforme, repris par
     * l'interface) : LONGUEUR, MAJUSCULE, MINUSCULE, CHIFFRE, SPECIAL, IDENTIFIANT. Liste vide s'il est
     * conforme ; le fournisseur d'identité reste juge en dernier ressort (historique notamment).
     */
    public static List<String> violations(PolitiqueSecurite p, String motDePasse, String identifiant) {
        List<String> v = new ArrayList<>();
        String m = motDePasse == null ? "" : motDePasse;
        if (m.length() < Math.max(p.longueurMinimale(), LONGUEUR_PLANCHER)) v.add("LONGUEUR");
        if (compter(m, Character::isUpperCase) < p.majusculesMin()) v.add("MAJUSCULE");
        if (compter(m, Character::isLowerCase) < p.minusculesMin()) v.add("MINUSCULE");
        if (compter(m, Character::isDigit) < p.chiffresMin()) v.add("CHIFFRE");
        if (compter(m, c -> !Character.isLetterOrDigit(c)) < p.speciauxMin()) v.add("SPECIAL");
        if (p.interditIdentifiant() && identifiant != null && !identifiant.isBlank() && m.toLowerCase(java.util.Locale.ROOT).contains(identifiant.toLowerCase(java.util.Locale.ROOT))) {
            v.add("IDENTIFIANT");
        }
        return v;
    }

    private static long compter(String m, java.util.function.IntPredicate test) {
        return m.codePoints().filter(test).count();
    }

    private static void ajouter(List<String> r, String regle, int valeur) {
        if (valeur > 0) {
            r.add(regle + "(" + valeur + ")");
        }
    }
}
