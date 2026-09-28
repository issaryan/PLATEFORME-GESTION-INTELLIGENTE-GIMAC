package cm.gimac.des.collaboration.repository;

import cm.gimac.des.collaboration.domain.ProcessedEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the ProcessedEvent entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {}
