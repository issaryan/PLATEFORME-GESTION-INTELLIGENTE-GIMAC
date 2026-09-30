package cm.gimac.des.collaboration.infrastructure.messaging.outbox;

import cm.gimac.des.collaboration.infrastructure.messaging.CloudEventEnvelope;
import cm.gimac.des.collaboration.infrastructure.messaging.TopicResolver;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/** Relaie l'outbox vers Kafka en enveloppe CloudEvents 1.0 (PI §0.4). */
@Component
public class OutboxRelay {

    private static final Logger LOG = LoggerFactory.getLogger(OutboxRelay.class);
    private static final int BATCH = 100;
    private static final int MAX_ATTEMPTS = 10;

    private final OutboxEventRepository repo;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper;
    private final TopicResolver topics;
    private final String source;

    public OutboxRelay(
        OutboxEventRepository repo,
        KafkaTemplate<String, String> kafka,
        ObjectMapper mapper,
        TopicResolver topics,
        @Value("${spring.application.name}") String app
    ) {
        this.repo = repo;
        this.kafka = kafka;
        this.mapper = mapper;
        this.topics = topics;
        this.source = "/gimac/des/" + app;
    }

    /** Relais toutes les 500 ms ; FOR UPDATE SKIP LOCKED permet plusieurs réplicas sans doublon. */
    @Scheduled(fixedDelayString = "${gimac.outbox.delay:500}")
    @Transactional
    public void relay() {
        for (OutboxEvent e : repo.lockPendingBatch(BATCH)) {
            try {
                String envelope = mapper.writeValueAsString(CloudEventEnvelope.of(e, source));
                kafka.send(topics.topicFor(e.getEventType()), e.getAggregateId(), envelope).get(5, TimeUnit.SECONDS);
                e.setStatus(OutboxEvent.Status.SENT);
                e.setSentAt(Instant.now());
            } catch (Exception ex) {
                if (ex instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                e.setAttempts(e.getAttempts() + 1);
                if (e.getAttempts() >= MAX_ATTEMPTS) {
                    e.setStatus(OutboxEvent.Status.FAILED); // alerte MS-MONITORING par métrique
                }
                LOG.warn("Relais de l'événement {} ({}) en échec, essai {} : {}", e.getId(), e.getEventType(), e.getAttempts(), ex.toString());
            }
        }
    }
}
