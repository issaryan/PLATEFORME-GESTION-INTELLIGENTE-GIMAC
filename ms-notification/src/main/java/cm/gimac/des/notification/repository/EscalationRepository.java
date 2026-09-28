package cm.gimac.des.notification.repository;

import cm.gimac.des.notification.domain.Escalation;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Escalation entity.
 */
@SuppressWarnings("unused")
@Repository
public interface EscalationRepository extends JpaRepository<Escalation, UUID>, JpaSpecificationExecutor<Escalation> {}
