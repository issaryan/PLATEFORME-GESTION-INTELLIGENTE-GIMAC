package cm.gimac.des.notification.domain.exception;

import java.util.Map;

/**
 * Erreur métier de MS-NOTIFICATION (D-80), convertie en ProblemDetail RFC 9457
 * (application/problem+json) dont le champ {@code code} (par exemple NOT_NOTIFICATION_INTROUVABLE) est
 * stable et lu par l'interface.
 */
public class NotificationException extends RuntimeException {

    private final String code;
    private final int status;
    private final transient Map<String, Object> details;

    private NotificationException(String code, int status, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.status = status;
        this.details = Map.copyOf(details);
    }

    /** 400 NOT_REQUETE_INVALIDE sur un champ. */
    public static NotificationException champ(String champ, String message) {
        return new NotificationException("NOT_REQUETE_INVALIDE", 400, message, Map.of("champ", champ));
    }

    /** 403 : jeton inadapté (par exemple sans sujet). */
    public static NotificationException interdit(String code, String message) {
        return new NotificationException(code, 403, message, Map.of());
    }

    /** 404 : notification inconnue ou d'un autre compte (son existence n'est pas révélée). */
    public static NotificationException introuvable(String code, Object id) {
        return new NotificationException(code, 404, "Ressource introuvable", Map.of("id", String.valueOf(id)));
    }

    /** 503 : service tiers indisponible, sans valeur supposée. */
    public static NotificationException indisponible(String code, String message) {
        return new NotificationException(code, 503, message, Map.of());
    }

    public String code() {
        return code;
    }

    public int status() {
        return status;
    }

    public Map<String, Object> details() {
        return details;
    }
}
