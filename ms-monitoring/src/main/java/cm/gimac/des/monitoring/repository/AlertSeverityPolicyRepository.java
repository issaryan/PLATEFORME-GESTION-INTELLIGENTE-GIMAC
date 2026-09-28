package cm.gimac.des.monitoring.repository;

import cm.gimac.des.monitoring.domain.AlertSeverityPolicy;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the AlertSeverityPolicy entity.
 */
@SuppressWarnings("unused")
@Repository
public interface AlertSeverityPolicyRepository extends JpaRepository<AlertSeverityPolicy, UUID> {}
