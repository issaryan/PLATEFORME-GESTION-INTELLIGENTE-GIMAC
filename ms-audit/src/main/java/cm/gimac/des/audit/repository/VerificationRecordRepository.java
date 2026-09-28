package cm.gimac.des.audit.repository;

import cm.gimac.des.audit.domain.VerificationRecord;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the VerificationRecord entity.
 */
@SuppressWarnings("unused")
@Repository
public interface VerificationRecordRepository
    extends JpaRepository<VerificationRecord, UUID>, JpaSpecificationExecutor<VerificationRecord> {}
