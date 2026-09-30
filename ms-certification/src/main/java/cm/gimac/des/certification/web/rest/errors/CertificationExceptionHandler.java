package cm.gimac.des.certification.web.rest.errors;

import cm.gimac.des.certification.domain.CertificationException;
import cm.gimac.des.certification.infrastructure.security.CorrelationContext;
import java.net.URI;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Erreurs métier en ProblemDetail RFC 9457 (application/problem+json) : code stable (CER_*,
 * IAM_REAUTH_REQUIRED, HOM_LIAISON_NOT_FOUND), message, détails et identifiant de corrélation, lus par le
 * client HTTP de l'interface (F0). Deux actes concurrents sur le même verrou : 409 CER_CONCURRENT_UPDATE.
 */
@RestControllerAdvice(basePackages = "cm.gimac.des.certification.web.rest.lot1")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CertificationExceptionHandler {

    @ExceptionHandler(CertificationException.class)
    public ResponseEntity<ProblemDetail> traiter(CertificationException ex) {
        return reponse(HttpStatus.valueOf(ex.status()), ex.code(), ex.getMessage(), ex.details());
    }

    @ExceptionHandler({ ObjectOptimisticLockingFailureException.class, DataIntegrityViolationException.class })
    public ResponseEntity<ProblemDetail> concurrence(RuntimeException ex) {
        return reponse(HttpStatus.CONFLICT, "CER_CONCURRENT_UPDATE", "Un autre acte vient d'être enregistré sur ce verrou : rechargez.", Map.of());
    }

    private static ResponseEntity<ProblemDetail> reponse(HttpStatus statut, String code, String message, Map<String, Object> details) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(statut, message);
        pd.setType(URI.create("https://des.gimac.local/problemes/" + code));
        pd.setTitle(code);
        pd.setProperty("code", code);
        pd.setProperty("message", message);
        pd.setProperty("details", details);
        pd.setProperty("correlationId", CorrelationContext.current());
        return ResponseEntity.status(statut).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(pd);
    }
}
