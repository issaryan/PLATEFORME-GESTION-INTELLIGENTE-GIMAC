package cm.gimac.des.notification.infrastructure.resilience;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Configuration Resilience4j pour le service Notifications.
 *
 * Instances configurées dans application.yml :
 *  CircuitBreaker : smsGateway (50% failure → OPEN 60s), smtpRelay (50% → OPEN 30s)
 *  Retry          : notificationsRetry (5 max, backoff x2 + jitter 50%)
 *  Bulkhead       : smtpBulkhead (pool 3-5, queue 20), smsBulkhead (pool 5-10, queue 50)
 *
 * L'annotation @CircuitBreaker / @Retry est directement appliquée sur les adapters
 * (SmtpEmailAdapter, SmsGatewayAdapter). Ce bean vérifie la disponibilité des registries
 * au démarrage et logue leur état.
 */
@ConditionalOnProperty(name = "gimac.notification.resilience.actif", havingValue = "true")
@Configuration
public class RetryStrategyConfig {

    private static final Logger log = LoggerFactory.getLogger(RetryStrategyConfig.class);

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;

    public RetryStrategyConfig(CircuitBreakerRegistry circuitBreakerRegistry,
                                RetryRegistry retryRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
    }

    @PostConstruct
    void logResilienceConfig() {
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb ->
            log.info("CircuitBreaker '{}' initialisé — état: {}", cb.getName(), cb.getState())
        );
        retryRegistry.getAllRetries().forEach(r ->
            log.info("Retry '{}' initialisé — maxAttempts: {}",
                r.getName(), r.getRetryConfig().getMaxAttempts())
        );
    }
}
