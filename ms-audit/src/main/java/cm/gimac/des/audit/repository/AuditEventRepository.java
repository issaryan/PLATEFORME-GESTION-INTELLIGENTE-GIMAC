package cm.gimac.des.audit.repository;

import cm.gimac.des.audit.domain.AuditEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the AuditEvent entity.
 */
@SuppressWarnings("unused")
@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID>, JpaSpecificationExecutor<AuditEvent> {}
