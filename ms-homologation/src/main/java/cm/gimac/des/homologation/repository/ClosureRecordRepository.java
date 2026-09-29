package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.ClosureRecord;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the ClosureRecord entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ClosureRecordRepository extends JpaRepository<ClosureRecord, UUID> {}
