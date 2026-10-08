package cm.gimac.des.pilotage.web.rest.errors;

import cm.gimac.des.pilotage.domain.exception.PilotageException;
import cm.gimac.des.pilotage.infrastructure.security.CorrelationContext;
import java.net.URI;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Erreurs métier de Mon activité en ProblemDetail RFC 9457 (application/problem+json) : code stable
 * (PIL_*), message, détails et identifiant de corrélation, lus par le client HTTP de l'interface ; erreurs champ par
 * champ ({@code details.fieldErrors}) aussi rendues en {@code fieldErrors}.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PilotageExceptionHandler {

    @ExceptionHandler(PilotageException.class)
    public ResponseEntity<ProblemDetail> traiter(PilotageException ex) {
        HttpStatus statut = HttpStatus.valueOf(ex.status());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(statut, ex.getMessage());
        pd.setType(URI.create("https://des.gimac.local/problemes/" + ex.code()));
        pd.setTitle(ex.code());
        pd.setProperty("code", ex.code());
        pd.setProperty("message", ex.getMessage());
        pd.setProperty("details", ex.details());
        // Erreurs champ par champ (formulaire de lancement, D-98 §2) : aussi au format fieldErrors de l'ExceptionTranslator,
        // repris par le client HTTP de l'interface dans details.champs, le code métier étant gardé.
        Object champs = ex.details().get("fieldErrors");
        if (champs != null) {
            pd.setProperty("fieldErrors", champs);
        }
        pd.setProperty("correlationId", CorrelationContext.current());
        return ResponseEntity.status(statut).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(pd);
    }
}
