package cm.gimac.des.collaboration.domain.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RG-52 : tout numéro de carte (PAN) d'un message est masqué avant l'enregistrement. Est un PAN
 * toute suite de 13 à 19 chiffres, espaces ou tirets admis entre les chiffres, qui passe le contrôle
 * de Luhn ; elle devient ses 6 premiers chiffres, des astérisques, puis ses 4 derniers.
 * Service de domaine pur, sans Spring.
 */
public final class MasquagePan {

    /** Suite de 13 à 19 chiffres, un espace ou un tiret au plus entre deux chiffres, bornée par des non-chiffres. */
    private static final Pattern CANDIDAT = Pattern.compile("(?<!\\d)\\d(?:[ -]?\\d){12,18}(?!\\d)");

    /** Texte masqué et nombre de numéros masqués. */
    public record Resultat(String texte, int masques) {}

    public Resultat masquer(String texte) {
        if (texte == null || texte.isEmpty()) {
            return new Resultat(texte, 0);
        }
        Matcher m = CANDIDAT.matcher(texte);
        StringBuilder sortie = new StringBuilder(texte.length());
        int masques = 0;
        while (m.find()) {
            String chiffres = m.group().replaceAll("[ -]", "");
            String remplacement = m.group();
            if (luhn(chiffres)) {
                remplacement = chiffres.substring(0, 6) + "*".repeat(chiffres.length() - 10) + chiffres.substring(chiffres.length() - 4);
                masques++;
            }
            m.appendReplacement(sortie, Matcher.quoteReplacement(remplacement));
        }
        m.appendTail(sortie);
        return new Resultat(sortie.toString(), masques);
    }

    /** Contrôle de Luhn (ISO/IEC 7812-1) sur une suite de chiffres. */
    public static boolean luhn(String chiffres) {
        int somme = 0;
        boolean doubler = false;
        for (int i = chiffres.length() - 1; i >= 0; i--) {
            int d = chiffres.charAt(i) - '0';
            if (doubler) {
                d *= 2;
                if (d > 9) {
                    d -= 9;
                }
            }
            somme += d;
            doubler = !doubler;
        }
        return somme % 10 == 0;
    }
}
