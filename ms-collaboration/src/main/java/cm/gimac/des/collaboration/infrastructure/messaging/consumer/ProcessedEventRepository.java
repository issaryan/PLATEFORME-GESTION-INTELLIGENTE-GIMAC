package cm.gimac.des.collaboration.infrastructure.messaging.consumer;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
    boolean existsByEventIdAndConsumerGroup(String eventId, String consumerGroup);
}
