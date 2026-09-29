package cm.gimac.des.document.web.rest.errors;

import cm.gimac.des.document.domain.DocumentException;
import cm.gimac.des.document.infrastructure.security.CorrelationContext;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Erreurs métier en ProblemDetail RFC 9457 : code stable (DOC_*), message, détails et identifiant
 * de corrélation, lus par le client HTTP de l'interface (F0).
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DocumentExceptionHandler {

    @ExceptionHandler(DocumentException.class)
    public ResponseEntity<ProblemDetail> traiter(DocumentException ex, HttpServletRequest requete) {
        return reponse(HttpStatus.valueOf(ex.status()), ex.code(), ex.getMessage(), ex.details(), requete);
    }

    /** Fichier refusé par le serveur avant le contrôleur (spring.servlet.multipart.max-file-size). */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ProblemDetail> traiter(MaxUploadSizeExceededException ex, HttpServletRequest requete) {
        Map<String, Object> details = ex.getMaxUploadSize() > 0 ? Map.of("tailleMax", ex.getMaxUploadSize()) : Map.of();
        return reponse(HttpStatus.PAYLOAD_TOO_LARGE, "DOC_FILE_TOO_LARGE", "Le fichier dépasse la taille maximale admise", details, requete);
    }

    private static ResponseEntity<ProblemDetail> reponse(
        HttpStatus statut,
        String code,
        String message,
        Map<String, Object> details,
        HttpServletRequest requete
    ) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(statut, message);
        pd.setType(URI.create("https://des.gimac.local/problemes/" + code));
        pd.setTitle(code);
        pd.setProperty("code", code);
        pd.setProperty("message", message);
        pd.setProperty("details", details);
        pd.setProperty("correlationId", CorrelationContext.depuis(requete.getHeader(CorrelationContext.HEADER)));
        return ResponseEntity.status(statut).body(pd);
    }
}
