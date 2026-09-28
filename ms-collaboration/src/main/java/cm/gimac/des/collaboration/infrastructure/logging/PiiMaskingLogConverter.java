package cm.gimac.des.collaboration.infrastructure.logging;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.regex.Pattern;

/**
 * Convertisseur Logback — masquage PII dans les messages de log.
 * Aucune donnée personnelle ni numéro de carte en clair dans les journaux
 * (loi camerounaise 2024/017, PCI-DSS v4.0.1 exigence 3.4, RG-52, RG-64).
 *
 * Enregistré sous le mot-clé %maskedMsg dans logback-spring.xml.
 * Appliqué avant le convertisseur CRLF : %crlf(%maskedMsg).
 *
 * Patterns couverts :
 *  - Numéros de compte (11 chiffres après "compte:" ou "compte=")
 *  - Numéros de téléphone camerounais (+237XXXXXXXXX)
 *  - Numéros de carte : 13 à 19 chiffres vérifiant la formule de Luhn, masqués en 6+4 (RG-52)
 *  - Adresses email (local-part avant @)
 */
public class PiiMaskingLogConverter extends MessageConverter {

    // Numéro de compte : 11 chiffres après "compte[:=] "
    private static final Pattern COMPTE_PATTERN =
        Pattern.compile("(?<=\\bcompte[:=]\\s?)\\d{11}");

    // Téléphone camerounais : +237 suivi de 9 chiffres
    private static final Pattern TEL_PATTERN =
        Pattern.compile("\\+237[0-9]{9}");

    // Numéro de carte bancaire : séquence de 13-19 chiffres (hors contextes déjà masqués)
    private static final Pattern CARTE_PATTERN =
        Pattern.compile("(?<!\\*{2})\\b\\d{13,19}\\b(?!\\*{2})");

    // Jeton Bearer (A09 — ne jamais logger de token en clair)
    private static final Pattern BEARER_PATTERN =
        Pattern.compile("(?i)(Bearer\\s+)[A-Za-z0-9\\-._~+/]+=*");

    // Email : local-part avant @ (caractères non-blancs et non-@)
    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("([^@\\s]{2,})(@[^\\s]+)");

    @Override
    public String convert(ILoggingEvent event) {
        String msg = super.convert(event);
        if (msg == null) {
            return null;
        }
        // Ordre d'application : du plus spécifique au plus général
        msg = BEARER_PATTERN.matcher(msg).replaceAll("$1[MASKED]");
        msg = COMPTE_PATTERN.matcher(msg).replaceAll("***********");
        msg = TEL_PATTERN.matcher(msg).replaceAll("+237*********");
        msg = CARTE_PATTERN.matcher(msg).replaceAll(m -> masquerCarte(m.group()));
        msg = EMAIL_PATTERN.matcher(msg).replaceAll(m -> "****" + m.group(2));
        return msg;
    }

    /**
     * Masque en 6+4 (six premiers et quatre derniers chiffres visibles, RG-52)
     * une suite de 13 à 19 chiffres vérifiant la formule de Luhn ; toute autre
     * suite (identifiant, horodatage) est laissée intacte.
     */
    static String masquerCarte(String chiffres) {
        if (!luhnValide(chiffres)) {
            return chiffres;
        }
        return chiffres.substring(0, 6) + "*".repeat(chiffres.length() - 10) + chiffres.substring(chiffres.length() - 4);
    }

    /** Formule de Luhn (ISO/IEC 7812-1). */
    static boolean luhnValide(String chiffres) {
        int somme = 0;
        boolean doubler = false;
        for (int i = chiffres.length() - 1; i >= 0; i--) {
            int c = chiffres.charAt(i) - '0';
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
