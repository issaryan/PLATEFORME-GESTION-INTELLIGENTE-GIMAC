package cm.gimac.des.notification.infrastructure.adapter.out;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import cm.gimac.des.notification.domain.NonRetryableException;
import cm.gimac.des.notification.infrastructure.adapter.out.port.SmsGatewayPort;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Adapter SMS HTTP — passerelle externe.
 *
 * Résilience (application.yml) :
 *  @CircuitBreaker(smsGateway) : 50% failure → OPEN 60s, sliding window 100
 *  @Retry(notificationsRetry)  : 5 tentatives max, backoff x2 + jitter 50%
 *
 * Cas d'erreur :
 *  - NumberParseException (numéro invalide)  → NonRetryableException → DLQ immédiate
 *  - HTTP 4xx                                → NonRetryableException → DLQ immédiate
 *  - HTTP 5xx / timeout                      → RuntimeException      → retry + TRANSIENT_FAILURE
 */
@ConditionalOnProperty(name = "gimac.notification.sms.actif", havingValue = "true")
@Component
@Profile("!stub")
public class SmsGatewayAdapter implements SmsGatewayPort {

    private static final Logger log = LoggerFactory.getLogger(SmsGatewayAdapter.class);

    private final WebClient webClient;
    private final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

    public SmsGatewayAdapter(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://sms-gateway-api").build();
    }

    @Override
    @CircuitBreaker(name = "smsGateway", fallbackMethod = "smsGatewayFallback")
    @Retry(name = "notificationsRetry")
    public void sendSms(String phoneNumberStr, String message) {
        // Validation E.164 — numéro invalide → DLQ immédiate (NonRetryableException)
        String formattedNumber;
        try {
            PhoneNumber phoneNumber = phoneNumberUtil.parse(phoneNumberStr, "CM");
            if (!phoneNumberUtil.isValidNumber(phoneNumber)) {
                throw new NonRetryableException(
                    "Numéro de téléphone invalide (non E.164) : validation échouée");
            }
            formattedNumber = phoneNumberUtil.format(phoneNumber, PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (NumberParseException e) {
            // Numéro non parseable → DLQ immédiate, pas de retry
            throw new NonRetryableException(
                "Numéro de téléphone non parseable : " + e.getErrorType().name());
        }

        webClient.post()
            .uri("/v1/sms/send")
            .bodyValue(Map.of("to", formattedNumber, "text", message))
            .retrieve()
            .onStatus(status -> status.is4xxClientError(),
                response -> Mono.error(new NonRetryableException(
                    "Erreur client 4xx de la gateway SMS — numéro ou paramètre invalide")))
            .onStatus(status -> status.is5xxServerError(),
                response -> Mono.error(new RuntimeException(
                    "Erreur serveur 5xx de la gateway SMS — retry planifié")))
            .bodyToMono(Void.class)
            .onErrorResume(NonRetryableException.class, e -> Mono.error(e))
            .block();
    }

    /**
     * Fallback Circuit Breaker ouvert → exception transitoire (retry + TRANSIENT_FAILURE).
     * Ne pas lever NonRetryableException ici : le circuit peut se refermer.
     */
    @SuppressWarnings("unused")
    void smsGatewayFallback(String phoneNumber, String message, Throwable t) {
        if (t instanceof NonRetryableException) {
            throw (NonRetryableException) t;
        }
        log.warn("SMS gateway circuit ouvert — fallback activé pour {}", phoneNumber);
        throw new RuntimeException("SMS gateway indisponible (circuit ouvert) : " + t.getMessage(), t);
    }
}
