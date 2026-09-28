package cm.gimac.des.reporting.repository;

import cm.gimac.des.reporting.domain.ProcessedEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the ProcessedEvent entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {}
