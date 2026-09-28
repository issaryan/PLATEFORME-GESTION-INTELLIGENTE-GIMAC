package cm.gimac.des.monitoring.repository;

import cm.gimac.des.monitoring.domain.OperatorUnavailability;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the OperatorUnavailability entity.
 */
@SuppressWarnings("unused")
@Repository
public interface OperatorUnavailabilityRepository
    extends JpaRepository<OperatorUnavailability, UUID>, JpaSpecificationExecutor<OperatorUnavailability> {}
