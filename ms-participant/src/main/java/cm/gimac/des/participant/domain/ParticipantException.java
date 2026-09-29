package cm.gimac.des.participant.domain;

import java.util.Map;

/**
 * Erreur métier de MS-PARTICIPANT, convertie en ProblemDetail RFC 9457 par
 * ParticipantExceptionHandler. Le code (par exemple PAR_TRANSITION_NOT_ALLOWED) et les détails
 * sont restitués au client, qui les affiche avec l'identifiant de corrélation.
 */
public class ParticipantException extends RuntimeException {

    private final String code;
    private final int status;
    private final transient Map<String, Object> details;

    private ParticipantException(String code, int status, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.status = status;
        this.details = Map.copyOf(details);
    }

    /** 409 : règle métier, prérequis manquant ou transition interdite. */
    public static ParticipantException conflict(String code, String message, Map<String, Object> details) {
        return new ParticipantException(code, 409, message, details);
    }

    /** 403 : action refusée à cet acteur (séparation des pouvoirs, RG-14). */
    public static ParticipantException forbidden(String code, String message) {
        return new ParticipantException(code, 403, message, Map.of());
    }

    /** 428 : réauthentification forte exigée et absente (IAM-07). */
    public static ParticipantException reauthRequired() {
        return new ParticipantException("PAR_REAUTH_REQUIRED", 428, "Réauthentification forte exigée", Map.of());
    }

    /** 503 : tiers indisponible, sans valeur par défaut. */
    public static ParticipantException unavailable(String code, String message) {
        return new ParticipantException(code, 503, message, Map.of());
    }

    public static ParticipantException notFound(String code, Object id) {
        return new ParticipantException(code, 404, "Ressource introuvable", Map.of("id", String.valueOf(id)));
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
