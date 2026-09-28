package cm.gimac.des.certification.repository;

import cm.gimac.des.certification.domain.ClosedOperationSnapshot;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the ClosedOperationSnapshot entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ClosedOperationSnapshotRepository
    extends JpaRepository<ClosedOperationSnapshot, UUID>, JpaSpecificationExecutor<ClosedOperationSnapshot> {}
