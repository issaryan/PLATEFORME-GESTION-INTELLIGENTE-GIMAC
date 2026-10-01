package cm.gimac.des.homologation.infrastructure.messaging.outbox;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    /** Verrouillage par lot : plusieurs réplicas relaient sans doublon (FOR UPDATE SKIP LOCKED). */
    @Query(
        value = "SELECT * FROM outbox_event WHERE status = 'PENDING' ORDER BY created_at LIMIT :n FOR UPDATE SKIP LOCKED",
        nativeQuery = true
    )
    List<OutboxEvent> lockPendingBatch(@Param("n") int n);

    List<OutboxEvent> findByEventTypeOrderByCreatedAtAsc(String eventType);

    /** Événements d'un agrégat (chronologie d'une version du référentiel). */
    List<OutboxEvent> findByAggregateIdOrderByCreatedAtAsc(String aggregateId);
}
