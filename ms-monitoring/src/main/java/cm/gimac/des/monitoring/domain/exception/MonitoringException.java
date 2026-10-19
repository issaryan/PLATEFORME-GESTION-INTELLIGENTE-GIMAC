package cm.gimac.des.monitoring.domain.exception;

import java.util.Map;

/**
 * Erreur métier de MS-MONITORING (D-114, docs/lot1/astreintes-monitoring.md §1), convertie en ProblemDetail RFC 9457
 * dont le champ {@code code} (par exemple MON_HORS_VACATION) est stable et traduit par l'interface. Le message rendu
 * est lu dans les catalogues {@code i18n/messages_{fr,en,es}.properties} (clé {@code erreur.<code>}), selon la langue
 * de la requête ; le message porté ici n'est qu'un repli en français.
 */
public class MonitoringException extends RuntimeException {

    private final String code;
    private final int status;
    private final transient Map<String, Object> details;

    private MonitoringException(String code, int status, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.status = status;
        this.details = Map.copyOf(details);
    }

    /** 400 : requête mal formée (champ absent, trop long, valeur inconnue). */
    public static MonitoringException invalide(String champ, String message) {
        return new MonitoringException("MON_REQUETE_INVALIDE", 400, message, Map.of("champ", champ));
    }

    /** 403 : acte réservé. */
    public static MonitoringException interdit(String code, String message) {
        return new MonitoringException(code, 403, message, Map.of());
    }

    /** 404 : ressource inconnue. */
    public static MonitoringException introuvable(String code, Object id) {
        return new MonitoringException(code, 404, "Ressource introuvable", Map.of("id", String.valueOf(id)));
    }

    /** 409 : état incompatible avec l'acte demandé. */
    public static MonitoringException conflit(String code, String message) {
        return new MonitoringException(code, 409, message, Map.of());
    }

    /** 422 : règle de gestion non respectée. */
    public static MonitoringException regle(String code, String message, Map<String, Object> details) {
        return new MonitoringException(code, 422, message, details);
    }

    public static MonitoringException regle(String code, String message) {
        return regle(code, message, Map.of());
    }

    /** 428 : acte engageant sans réauthentification forte (en-tête X-Reauth-Assertion absent, IAM-07). */
    public static MonitoringException preconditionRequise(String code, String message) {
        return new MonitoringException(code, 428, message, Map.of());
    }

    /** 503 : service tiers indisponible, sans valeur supposée. */
    public static MonitoringException indisponible(String code, String message) {
        return new MonitoringException(code, 503, message, Map.of());
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
