package cm.gimac.des.collaboration.domain.exception;

import java.util.Map;

/**
 * Erreur métier de MS-COLLABORATION, convertie en ProblemDetail RFC 9457 (application/problem+json)
 * dont le champ {@code code} (par exemple COL_MESSAGE_EMPTY) est stable et lu par l'interface.
 */
public class CollaborationException extends RuntimeException {

    private final String code;
    private final int status;
    private final transient Map<String, Object> details;

    private CollaborationException(String code, int status, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.status = status;
        this.details = Map.copyOf(details);
    }

    /** 400 : requête contraire à une règle de la messagerie (D-64). */
    public static CollaborationException invalide(String code, String message) {
        return invalide(code, message, Map.of());
    }

    public static CollaborationException invalide(String code, String message, Map<String, Object> details) {
        return new CollaborationException(code, 400, message, details);
    }

    /** 422 : requête bien formée mais contraire à la conversation visée (D-82 : réponse ou mention invalide). */
    public static CollaborationException nonTraitable(String code, String message, Map<String, Object> details) {
        return new CollaborationException(code, 422, message, details);
    }

    /** 404 : ressource absente ou hors de portée de l'appelant (on ne révèle pas son existence). */
    public static CollaborationException introuvable(String code, Object id) {
        return new CollaborationException(code, 404, "Ressource introuvable", Map.of("id", String.valueOf(id)));
    }

    /** 403 : acte réservé (jeton utilisateur exigé). */
    public static CollaborationException interdit(String code, String message) {
        return new CollaborationException(code, 403, message, Map.of());
    }

    /** 503 : service tiers indisponible, sans valeur supposée. */
    public static CollaborationException indisponible(String code, String message) {
        return new CollaborationException(code, 503, message, Map.of());
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
