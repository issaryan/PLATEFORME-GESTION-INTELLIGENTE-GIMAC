package cm.gimac.des.monitoring.infrastructure.logging;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.util.regex.Pattern;

/**
 * Convertisseur Logback — masquage PII dans les messages de log.
 * Loi camerounaise 2024/017 : aucune donnée personnelle en clair dans les logs.
 *
 * Enregistré sous le mot-clé %maskedMsg dans logback-spring.xml.
 * Appliqué avant le convertisseur CRLF : %crlf(%maskedMsg).
 *
 * Patterns couverts :
 *  - Numéros de compte Afriland (11 chiffres après "compte:" ou "compte=")
 *  - Numéros de téléphone camerounais (+237XXXXXXXXX)
 *  - Numéros de carte bancaire (13-19 chiffres consécutifs)
 *  - Adresses email (local-part avant @)
 */
public class PiiMaskingLogConverter extends MessageConverter {

    // Numéro de compte Afriland — 11 chiffres après "compte[:=] "
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
        msg = CARTE_PATTERN.matcher(msg).replaceAll(m -> {
            String s = m.group();
            if (s.length() <= 4) return s;
            return "*".repeat(s.length() - 4) + s.substring(s.length() - 4);
        });
        msg = EMAIL_PATTERN.matcher(msg).replaceAll(m -> "****" + m.group(2));
        return msg;
    }
}
