package cm.gimac.des.notification.repository;

import cm.gimac.des.notification.domain.ConsolidationWindow;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the ConsolidationWindow entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ConsolidationWindowRepository extends JpaRepository<ConsolidationWindow, UUID> {}
