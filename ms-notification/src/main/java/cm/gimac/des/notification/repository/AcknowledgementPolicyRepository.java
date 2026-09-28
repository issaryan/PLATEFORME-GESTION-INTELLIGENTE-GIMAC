package cm.gimac.des.notification.repository;

import cm.gimac.des.notification.domain.AcknowledgementPolicy;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the AcknowledgementPolicy entity.
 */
@SuppressWarnings("unused")
@Repository
public interface AcknowledgementPolicyRepository extends JpaRepository<AcknowledgementPolicy, UUID> {}
