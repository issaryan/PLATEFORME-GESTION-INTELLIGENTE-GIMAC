package cm.gimac.des.iam.web.rest.errors;

import cm.gimac.des.iam.domain.IamException;
import cm.gimac.des.iam.infrastructure.security.CorrelationContext;
import java.net.URI;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Erreurs métier en ProblemDetail RFC 9457 : code stable (IAM_*), message, détails, corrélation. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IamExceptionHandler {

    @ExceptionHandler(IamException.class)
    public ResponseEntity<ProblemDetail> traiter(IamException ex) {
        HttpStatus statut = HttpStatus.valueOf(ex.status());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(statut, ex.getMessage());
        pd.setType(URI.create("https://des.gimac.local/problemes/" + ex.code()));
        pd.setTitle(ex.code());
        pd.setProperty("code", ex.code());
        pd.setProperty("message", ex.getMessage());
        pd.setProperty("details", ex.details());
        // Politique des mots de passe (D-65) : violations aussi au premier niveau (contrat de la connexion).
        if (ex.details().containsKey("violations")) {
            pd.setProperty("violations", ex.details().get("violations"));
        }
        pd.setProperty("correlationId", CorrelationContext.current());
        return ResponseEntity.status(statut).body(pd);
    }
}
