package cm.gimac.des.monitoring.web.rest.errors;

import cm.gimac.des.monitoring.domain.exception.MonitoringException;
import cm.gimac.des.monitoring.infrastructure.security.CorrelationContext;
import java.net.URI;
import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Erreurs métier de la supervision et des astreintes en ProblemDetail RFC 9457 (application/problem+json) : code
 * stable (MON_*), message dans la langue de la requête (catalogues {@code i18n/messages_*.properties}, clé
 * {@code erreur.<code>}), détails et identifiant de corrélation.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MonitoringExceptionHandler {

    private final MessageSource messages;

    public MonitoringExceptionHandler(MessageSource messages) {
        this.messages = messages;
    }

    @ExceptionHandler(MonitoringException.class)
    public ResponseEntity<ProblemDetail> traiter(MonitoringException ex, Locale langue) {
        HttpStatus statut = HttpStatus.valueOf(ex.status());
        String message = messages.getMessage("erreur." + ex.code(), null, ex.getMessage(), langue == null ? Locale.FRENCH : langue);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(statut, message);
        pd.setType(URI.create("https://des.gimac.local/problemes/" + ex.code()));
        pd.setTitle(ex.code());
        pd.setProperty("code", ex.code());
        pd.setProperty("message", message);
        pd.setProperty("details", ex.details());
        pd.setProperty("correlationId", CorrelationContext.current());
        return ResponseEntity.status(statut).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(pd);
    }
}
