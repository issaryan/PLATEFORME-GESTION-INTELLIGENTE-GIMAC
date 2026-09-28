package cm.gimac.des.notification.repository;

import cm.gimac.des.notification.domain.DeliveryTrace;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the DeliveryTrace entity.
 */
@SuppressWarnings("unused")
@Repository
public interface DeliveryTraceRepository extends JpaRepository<DeliveryTrace, UUID>, JpaSpecificationExecutor<DeliveryTrace> {}
