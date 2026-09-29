package cm.gimac.des.document.domain.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Contrôle de masquage au dépôt (BNF-16, RG-52, tableau 6.18 du CC) : recherche, dans le texte
 * d'une pièce, des numéros de carte plausibles, c'est-à-dire des suites de 13 à 19 chiffres
 * vérifiant la formule de Luhn, contiguës ou groupées par quatre (espaces ou tirets). Un numéro
 * déjà masqué en 6+4 (astérisques) n'est pas retenu. Même règle que le masquage des journaux
 * (PiiMaskingLogConverter), qui s'appuie sur cette classe.
 */
public final class ControleNumerosCarte {

    /** Suite contiguë de 13 à 19 chiffres, non collée à un autre chiffre ni à un astérisque. */
    private static final Pattern CONTIGU = Pattern.compile("(?<![0-9*])\\d{13,19}(?![0-9*])");

    /** Groupes de quatre chiffres séparés par un même espace ou tiret, dernier groupe de 1 à 7 chiffres. */
    private static final Pattern GROUPE = Pattern.compile("(?<![0-9*])\\d{4}([ -])\\d{4}\\1\\d{4}\\1\\d{1,7}(?![0-9*])");

    private ControleNumerosCarte() {}

    /** Nombre de numéros de carte plausibles (Luhn, 13 à 19 chiffres) présents dans le texte. */
    public static int compter(String texte) {
        if (texte == null || texte.isEmpty()) {
            return 0;
        }
        return compter(CONTIGU.matcher(texte)) + compter(GROUPE.matcher(texte));
    }

    private static int compter(Matcher m) {
        int n = 0;
        while (m.find()) {
            String chiffres = m.group().replace(" ", "").replace("-", "");
            if (chiffres.length() >= 13 && chiffres.length() <= 19 && luhnValide(chiffres)) {
                n++;
            }
        }
        return n;
    }

    /**
     * Masque en 6+4 (six premiers et quatre derniers chiffres visibles, RG-52) une suite de 13 à
     * 19 chiffres vérifiant la formule de Luhn ; toute autre suite est rendue intacte.
     */
    public static String masquer(String chiffres) {
        if (chiffres == null || chiffres.length() < 13 || chiffres.length() > 19 || !luhnValide(chiffres)) {
            return chiffres;
        }
        return chiffres.substring(0, 6) + "*".repeat(chiffres.length() - 10) + chiffres.substring(chiffres.length() - 4);
    }

    /** Formule de Luhn (ISO/IEC 7812-1) ; faux pour toute chaîne contenant autre chose que des chiffres. */
    public static boolean luhnValide(String chiffres) {
        int somme = 0;
        boolean doubler = false;
        for (int i = chiffres.length() - 1; i >= 0; i--) {
            int c = chiffres.charAt(i) - '0';
            if (c < 0 || c > 9) {
                return false;
            }
            if (doubler) {
                c *= 2;
                if (c > 9) {
                    c -= 9;
                }
            }
            somme += c;
            doubler = !doubler;
        }
        return somme % 10 == 0;
    }
}
