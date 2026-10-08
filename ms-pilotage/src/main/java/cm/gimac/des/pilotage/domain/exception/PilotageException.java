package cm.gimac.des.pilotage.domain.exception;

import java.util.Map;

/**
 * Erreur métier de MS-PILOTAGE (Mon activité, D-76), convertie en ProblemDetail RFC 9457
 * (application/problem+json) dont le champ {@code code} (par exemple PIL_HORS_EQUIPE) est stable et lu
 * par l'interface.
 */
public class PilotageException extends RuntimeException {

    private final String code;
    private final int status;
    private final transient Map<String, Object> details;

    private PilotageException(String code, int status, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.status = status;
        this.details = Map.copyOf(details);
    }

    /** 400 : requête mal formée (champ absent, trop long, valeur inconnue). */
    public static PilotageException invalide(String code, String message, Map<String, Object> details) {
        return new PilotageException(code, 400, message, details);
    }

    /** 400 PIL_REQUETE_INVALIDE sur un champ. */
    public static PilotageException champ(String champ, String message) {
        return invalide("PIL_REQUETE_INVALIDE", message, Map.of("champ", champ));
    }

    /** 403 : acte réservé ou hors équipe. */
    public static PilotageException interdit(String code, String message) {
        return new PilotageException(code, 403, message, Map.of());
    }

    /** 403 avec détails (règles d'initiateurs d'un processus refusé, D-98). */
    public static PilotageException interditDetaille(String code, String message, Map<String, Object> details) {
        return new PilotageException(code, 403, message, details);
    }

    /** 403 PIL_HORS_EQUIPE (docs/lot1/mon-activite.md §3). */
    public static PilotageException horsEquipe() {
        return interdit("PIL_HORS_EQUIPE", "Ce compte n'est pas dans votre équipe");
    }

    /** 403 PIL_ACTION_RESERVEE : la donnée est lisible, l'acte est réservé à son propriétaire. */
    public static PilotageException reservee(String message) {
        return interdit("PIL_ACTION_RESERVEE", message);
    }

    /** 404 : ressource inconnue. */
    public static PilotageException introuvable(String code, Object id) {
        return new PilotageException(code, 404, "Ressource introuvable", Map.of("id", String.valueOf(id)));
    }

    /** 409 : état incompatible avec l'acte demandé. */
    public static PilotageException conflit(String code, String message, Map<String, Object> details) {
        return new PilotageException(code, 409, message, details);
    }

    /** 422 : règle de gestion non respectée. */
    public static PilotageException regle(String code, String message, Map<String, Object> details) {
        return new PilotageException(code, 422, message, details);
    }

    public static PilotageException regle(String code, String message) {
        return regle(code, message, Map.of());
    }

    /** 428 : acte engageant sans réauthentification forte (en-tête X-Reauth-Assertion absent, IAM-07). */
    public static PilotageException preconditionRequise(String code, String message) {
        return new PilotageException(code, 428, message, Map.of());
    }

    /** 429 : acte répété trop tôt (relance d'une tâche, une par 24 h). */
    public static PilotageException tropTot(String code, String message, Map<String, Object> details) {
        return new PilotageException(code, 429, message, details);
    }

    /** 503 : service tiers indisponible, sans valeur supposée. */
    public static PilotageException indisponible(String code, String message) {
        return new PilotageException(code, 503, message, Map.of());
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
