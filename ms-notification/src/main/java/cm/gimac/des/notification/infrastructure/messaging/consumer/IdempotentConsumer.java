package cm.gimac.des.notification.infrastructure.messaging.consumer;

import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Consommateur idempotent (PI §0.4, RG-15, RG-35). */
@Component
public class IdempotentConsumer {

    private final ProcessedEventRepository processed;

    public IdempotentConsumer(ProcessedEventRepository processed) {
        this.processed = processed;
    }

    /** Exécute le traitement une seule fois par (eventId, groupe) ; un doublon est acquitté sans effet. */
    @Transactional
    public boolean handleOnce(String eventId, String consumerGroup, Runnable handler) {
        if (processed.existsByEventIdAndConsumerGroup(eventId, consumerGroup)) {
            return false;
        }
        handler.run();
        processed.save(new ProcessedEvent(eventId, consumerGroup, Instant.now()));
        return true;
    }
}
