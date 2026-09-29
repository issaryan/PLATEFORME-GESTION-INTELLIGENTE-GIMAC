package cm.gimac.des.document.domain;

import java.util.Map;

/**
 * Erreur métier de MS-DOCUMENT, convertie en ProblemDetail RFC 9457 par
 * DocumentExceptionHandler. Le code (par exemple DOC_FILE_TOO_LARGE) et les détails sont
 * restitués au client, qui les affiche avec l'identifiant de corrélation.
 */
public class DocumentException extends RuntimeException {

    private final String code;
    private final int status;
    private final transient Map<String, Object> details;

    private DocumentException(String code, int status, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.status = status;
        this.details = Map.copyOf(details);
    }

    /** 400 : requête invalide (fichier absent ou vide, finalité ou référence mal formée). */
    public static DocumentException invalide(String code, String message) {
        return new DocumentException(code, 400, message, Map.of());
    }

    /** 403 : action refusée à cet appelant (jeton utilisateur exigé). */
    public static DocumentException interdit(String code, String message) {
        return new DocumentException(code, 403, message, Map.of());
    }

    /** 413 : fichier au-delà de la taille admise. */
    public static DocumentException tropVolumineux(long taille, long tailleMax) {
        return new DocumentException(
            "DOC_FILE_TOO_LARGE",
            413,
            "Le fichier dépasse la taille maximale admise",
            Map.of("taille", taille, "tailleMax", tailleMax)
        );
    }

    /** 422 : contenu refusé par la chaîne de contrôle au dépôt (numéro de carte, PDF illisible). */
    public static DocumentException refuse(String code, String message, Map<String, Object> details) {
        return new DocumentException(code, 422, message, details);
    }

    /** 404 : document inconnu. */
    public static DocumentException introuvable(Object id) {
        return new DocumentException("DOC_NOT_FOUND", 404, "Document introuvable", Map.of("id", String.valueOf(id)));
    }

    /** 503 : dépôt objet indisponible, sans valeur par défaut. */
    public static DocumentException indisponible(String code, String message) {
        return new DocumentException(code, 503, message, Map.of());
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
