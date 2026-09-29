package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.RejectedRecord;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the RejectedRecord entity.
 */
@SuppressWarnings("unused")
@Repository
public interface RejectedRecordRepository extends JpaRepository<RejectedRecord, UUID> {}
