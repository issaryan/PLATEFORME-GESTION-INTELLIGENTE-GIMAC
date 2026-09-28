package cm.gimac.des.incident.repository;

import cm.gimac.des.incident.domain.RemediationAction;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the RemediationAction entity.
 */
@SuppressWarnings("unused")
@Repository
public interface RemediationActionRepository extends JpaRepository<RemediationAction, UUID>, JpaSpecificationExecutor<RemediationAction> {}
