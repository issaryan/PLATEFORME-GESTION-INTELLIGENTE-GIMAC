package cm.gimac.des.certification.repository;

import cm.gimac.des.certification.domain.ChangeRecord;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the ChangeRecord entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ChangeRecordRepository extends JpaRepository<ChangeRecord, UUID>, JpaSpecificationExecutor<ChangeRecord> {}
