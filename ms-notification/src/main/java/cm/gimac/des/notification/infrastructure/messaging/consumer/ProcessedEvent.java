package cm.gimac.des.notification.infrastructure.messaging.consumer;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** Trace d'un événement déjà traité par un groupe de consommateurs (RG-15, RG-35). */
@Entity
@Table(name = "processed_event")
public class ProcessedEvent {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(name = "consumer_group", nullable = false)
    private String consumerGroup;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    protected ProcessedEvent() {}

    public ProcessedEvent(String eventId, String consumerGroup, Instant processedAt) {
        this.eventId = eventId;
        this.consumerGroup = consumerGroup;
        this.processedAt = processedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
