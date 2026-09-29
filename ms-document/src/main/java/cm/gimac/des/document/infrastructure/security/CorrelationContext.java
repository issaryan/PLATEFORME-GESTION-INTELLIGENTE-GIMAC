package cm.gimac.des.document.infrastructure.security;

import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;

/**
 * Identifiant de corrélation de la requête courante (REC-GIMAC-01 §6 point 3) : en-tête
 * X-Correlation-Id posé par la passerelle s'il est bien formé, sinon celui du contexte de
 * journalisation (filtre de corrélation), sinon un identifiant neuf.
 */
public final class CorrelationContext {

    public static final String HEADER = "X-Correlation-Id";
    /** Clé du contexte de journalisation, celle du filtre TenantAndCorrelationFilter. */
    public static final String MDC_KEY = "correlation_id";
    public static final String INSTITUTION_CLAIM = "institution_id";

    private static final Pattern FORMAT = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");

    private CorrelationContext() {}

    /** Identifiant de corrélation : en-tête reçu, puis contexte de journalisation, puis identifiant neuf. */
    public static String depuis(String entete) {
        if (entete != null && FORMAT.matcher(entete).matches()) {
            return entete;
        }
        String id = MDC.get(MDC_KEY);
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
            MDC.put(MDC_KEY, id);
        }
        return id;
    }
}
