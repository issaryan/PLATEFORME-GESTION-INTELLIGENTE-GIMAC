package cm.gimac.des.collaboration.web.rest.errors;

import cm.gimac.des.collaboration.domain.exception.CollaborationException;
import cm.gimac.des.collaboration.domain.service.ReglesMessagerie;
import cm.gimac.des.collaboration.infrastructure.security.CorrelationContext;
import java.net.URI;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Erreurs métier en ProblemDetail RFC 9457 (application/problem+json) : code stable (COL_*),
 * message, détails et identifiant de corrélation, lus par le client HTTP de l'interface.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CollaborationExceptionHandler {

    @ExceptionHandler(CollaborationException.class)
    public ResponseEntity<ProblemDetail> traiter(CollaborationException ex) {
        HttpStatus statut = HttpStatus.valueOf(ex.status());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(statut, ex.getMessage());
        pd.setType(URI.create("https://des.gimac.local/problemes/" + ex.code()));
        pd.setTitle(ex.code());
        pd.setProperty("code", ex.code());
        pd.setProperty("message", ex.getMessage());
        pd.setProperty("details", ex.details());
        pd.setProperty("correlationId", CorrelationContext.current());
        return ResponseEntity.status(statut).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(pd);
    }

    /** Requête multipart au-delà des limites du conteneur : même code que la règle des 20 Mo. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ProblemDetail> tropVolumineux(MaxUploadSizeExceededException ex) {
        return traiter(
            CollaborationException.invalide(
                "COL_ATTACHMENT_TOO_LARGE",
                "Pièce jointe au-delà de 20 Mo",
                Map.of("maximum", ReglesMessagerie.PIECE_TAILLE_MAX)
            )
        );
    }
}
