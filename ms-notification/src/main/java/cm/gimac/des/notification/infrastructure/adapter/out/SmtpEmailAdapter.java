package cm.gimac.des.notification.infrastructure.adapter.out;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import cm.gimac.des.notification.infrastructure.adapter.out.port.EmailGatewayPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;

/**
 * Adapter SMTP — envoi email via JavaMailSender.
 *
 * Résilience (application.yml) :
 *  @CircuitBreaker(smtpRelay) : 50% failure → OPEN 30s, sliding window 20
 *  @Retry(notificationsRetry) : 5 tentatives max, backoff x2 + jitter 50%
 *
 * Toute exception est transitoire (pas de NonRetryableException SMTP — le serveur
 * peut être temporairement indisponible). DLQ après épuisement des retries.
 */
@ConditionalOnProperty(name = "gimac.notification.courriel.actif", havingValue = "true")
@Component
public class SmtpEmailAdapter implements EmailGatewayPort {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailAdapter.class);

    private final JavaMailSender javaMailSender;

    public SmtpEmailAdapter(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Override
    @CircuitBreaker(name = "smtpRelay", fallbackMethod = "smtpRelayFallback")
    @Retry(name = "notificationsRetry")
    public void sendEmail(String to, String subject, String body) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());
            helper.setTo(to);
            helper.setSubject(subject != null ? subject : "");
            helper.setText(body, true);
            javaMailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Erreur envoi email SMTP vers " + to + " : " + e.getMessage(), e);
        }
    }

    /**
     * Fallback Circuit Breaker ouvert — toujours transitoire pour SMTP.
     */
    void smtpRelayFallback(String to, String subject, String body, Throwable t) {
        log.warn("SMTP circuit ouvert — fallback activé pour {}", to);
        throw new RuntimeException("SMTP relay indisponible (circuit ouvert) : " + t.getMessage(), t);
    }
}
