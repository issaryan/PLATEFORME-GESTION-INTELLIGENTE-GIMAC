package cm.gimac.des.homologation.domain;

import java.util.Map;

/**
 * Erreur métier de MS-HOMOLOGATION, convertie en ProblemDetail RFC 9457 (PI-GIMAC-01, HOM-01 A2).
 * Le code (par exemple HOM_REFERENTIAL_FROZEN) et les détails sont restitués au client.
 */
public class HomologationException extends RuntimeException {

    private final String code;
    private final int status;
    private final transient Map<String, Object> details;

    private HomologationException(String code, int status, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.status = status;
        this.details = Map.copyOf(details);
    }

    /** 409 : règle métier ou transition interdite. */
    public static HomologationException conflict(String code, String message, Map<String, Object> details) {
        return new HomologationException(code, 409, message, details);
    }

    /** 422 : demande bien formée mais irrecevable (preuve incomplète, valeur invalide). */
    public static HomologationException unprocessable(String code, String message) {
        return new HomologationException(code, 422, message, Map.of());
    }

    /** 428 : décision engageante sans réauthentification forte (IAM-07, en-tête X-Reauth-Assertion). */
    public static HomologationException reauthRequired() {
        return new HomologationException("IAM_REAUTH_REQUIRED", 428, "Réauthentification forte exigée", Map.of());
    }

    /** 404 avec un message explicite (portail : liaison d'une autre institution). */
    public static HomologationException notFound(String code, String message, Object id) {
        return new HomologationException(code, 404, message, Map.of("id", String.valueOf(id)));
    }

    /** 503 : tiers indisponible, sans valeur par défaut. */
    public static HomologationException unavailable(String code, String message) {
        return new HomologationException(code, 503, message, Map.of());
    }

    /** 403 : acte réservé à une autre fonction ou à un autre titulaire. */
    public static HomologationException forbidden(String code, String message) {
        return new HomologationException(code, 403, message, Map.of());
    }

    public static HomologationException notFound(String code, Object id) {
        return new HomologationException(code, 404, "Ressource introuvable", Map.of("id", String.valueOf(id)));
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
