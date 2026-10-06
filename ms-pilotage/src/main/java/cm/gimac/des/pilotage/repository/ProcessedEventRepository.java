package cm.gimac.des.pilotage.repository;

import cm.gimac.des.pilotage.domain.ProcessedEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the ProcessedEvent entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {
    /** Événement déjà consommé (idempotence, RG-15). */
    boolean existsByEventId(UUID eventId);
}
