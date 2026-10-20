package cm.gimac.des.iam.domain;

import java.util.Map;

/**
 * Erreur métier de MS-IAM, convertie en ProblemDetail RFC 9457 par IamExceptionHandler. Le code
 * (par exemple IAM_SELF_ADMINISTRATION) et les détails sont restitués au client.
 */
public class IamException extends RuntimeException {

    private final String code;
    private final int status;
    private final transient Map<String, Object> details;

    private IamException(String code, int status, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.status = status;
        this.details = Map.copyOf(details);
    }

    /** 400 : donnée d'entrée refusée par une règle (profil inconnu, politique incohérente). */
    public static IamException invalide(String code, String message, Map<String, Object> details) {
        return new IamException(code, 400, message, details);
    }

    /** 409 : conflit avec l'état (identifiant déjà pris, dernier super administrateur). */
    public static IamException conflit(String code, String message, Map<String, Object> details) {
        return new IamException(code, 409, message, details);
    }

    /**
     * 422 : demande bien formée mais inapplicable à l'état des données (unité inconnue ou inactive,
     * responsable invalide, compte d'institution, docs/lot1/mon-activite.md §1.2).
     */
    public static IamException nonTraitable(String code, String message, Map<String, Object> details) {
        return new IamException(code, 422, message, details);
    }

    /** 403 : action refusée à cet acteur (séparation des pouvoirs, profil privilégié). */
    public static IamException interdit(String code, String message) {
        return new IamException(code, 403, message, Map.of());
    }

    /** 403 avec le détail de ce qui est refusé (D-115 : rôles hors de la portée de l'institution). */
    public static IamException interdit(String code, String message, Map<String, Object> details) {
        return new IamException(code, 403, message, details);
    }

    /** 428 : réauthentification forte exigée et absente (IAM-07). */
    public static IamException reauthRequise() {
        return new IamException("IAM_REAUTH_REQUIRED", 428, "Réauthentification forte exigée", Map.of());
    }

    /** 503 : fournisseur d'identité indisponible, sans valeur par défaut. */
    public static IamException indisponible(String code, String message) {
        return new IamException(code, 503, message, Map.of());
    }

    public static IamException introuvable(String code, Object id) {
        return new IamException(code, 404, "Ressource introuvable", Map.of("id", String.valueOf(id)));
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
