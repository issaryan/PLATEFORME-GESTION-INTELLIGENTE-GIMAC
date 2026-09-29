package cm.gimac.des.homologation.web.rest.errors;

import cm.gimac.des.homologation.domain.HomologationException;
import cm.gimac.des.homologation.infrastructure.security.CorrelationContext;
import java.net.URI;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Erreurs métier en ProblemDetail RFC 9457 (HOM-01 A2) : code stable (HOM_*), message,
 * détails et identifiant de corrélation, lus par le client HTTP de l'interface (F0).
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HomologationExceptionHandler {

    @ExceptionHandler(HomologationException.class)
    public ResponseEntity<ProblemDetail> traiter(HomologationException ex) {
        HttpStatus statut = HttpStatus.valueOf(ex.status());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(statut, ex.getMessage());
        pd.setType(URI.create("https://des.gimac.local/problemes/" + ex.code()));
        pd.setTitle(ex.code());
        pd.setProperty("code", ex.code());
        pd.setProperty("message", ex.getMessage());
        pd.setProperty("details", ex.details());
        pd.setProperty("correlationId", CorrelationContext.current());
        return ResponseEntity.status(statut).body(pd);
    }
}
