package cm.gimac.des.certification.domain;

import java.util.Map;

/**
 * Erreur métier de MS-CERTIFICATION, convertie en ProblemDetail RFC 9457 (application/problem+json)
 * par CertificationExceptionHandler. Le code (CER_*, IAM_REAUTH_REQUIRED, HOM_LIAISON_NOT_FOUND) est
 * celui du serveur de substitution de l'interface, qui fait foi (docs/lot1/parcours-backend.md §4).
 */
public class CertificationException extends RuntimeException {

    private final String code;
    private final int status;
    private final transient Map<String, Object> details;

    private CertificationException(String code, int status, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.status = status;
        this.details = Map.copyOf(details);
    }

    /** 409 : règle de gestion ou état qui interdit l'action. */
    public static CertificationException conflit(String code, String message) {
        return new CertificationException(code, 409, message, Map.of());
    }

    /** 409 avec détails. */
    public static CertificationException conflit(String code, String message, Map<String, Object> details) {
        return new CertificationException(code, 409, message, details);
    }

    /** 422 : donnée reçue irrecevable (référence mal formée, motif absent, format de pièce). */
    public static CertificationException irrecevable(String code, String message) {
        return new CertificationException(code, 422, message, Map.of());
    }

    /** 404 : liaison inconnue ou hors du périmètre de l'institution du jeton (portail). */
    public static CertificationException introuvable(String code, String message) {
        return new CertificationException(code, 404, message, Map.of());
    }

    /** 401 : réauthentification forte exigée et absente (IAM-07, en-tête X-Reauth-Assertion). */
    public static CertificationException reauthentificationExigee() {
        return new CertificationException("IAM_REAUTH_REQUIRED", 401, "Réauthentification exigée.", Map.of());
    }

    /** 403 : action refusée à cet acteur (assertion invalide, séparation des pouvoirs). */
    public static CertificationException interdit(String code, String message) {
        return new CertificationException(code, 403, message, Map.of());
    }

    /** 503 : service tiers indisponible, sans valeur par défaut. */
    public static CertificationException indisponible(String code, String message) {
        return new CertificationException(code, 503, message, Map.of());
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
