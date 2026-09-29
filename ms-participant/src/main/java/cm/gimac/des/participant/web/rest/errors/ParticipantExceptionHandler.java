package cm.gimac.des.participant.web.rest.errors;

import cm.gimac.des.participant.domain.ParticipantException;
import cm.gimac.des.participant.infrastructure.security.CorrelationContext;
import java.net.URI;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Erreurs métier en ProblemDetail RFC 9457 : code stable (PAR_*), message, détails et identifiant
 * de corrélation, lus par le client HTTP de l'interface (F0).
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ParticipantExceptionHandler {

    @ExceptionHandler(ParticipantException.class)
    public ResponseEntity<ProblemDetail> traiter(ParticipantException ex) {
        return reponse(HttpStatus.valueOf(ex.status()), ex.code(), ex.getMessage(), ex.details());
    }

    /** Course entre deux enregistrements concurrents : la contrainte d'unicité de la base tranche. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> traiter(DataIntegrityViolationException ex) {
        return reponse(HttpStatus.CONFLICT, "PAR_CODE_ALREADY_USED", "Code déjà attribué à une autre institution", Map.of());
    }

    private static ResponseEntity<ProblemDetail> reponse(HttpStatus statut, String code, String message, Map<String, Object> details) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(statut, message);
        pd.setType(URI.create("https://des.gimac.local/problemes/" + code));
        pd.setTitle(code);
        pd.setProperty("code", code);
        pd.setProperty("message", message);
        pd.setProperty("details", details);
        pd.setProperty("correlationId", CorrelationContext.current());
        return ResponseEntity.status(statut).body(pd);
    }
}
