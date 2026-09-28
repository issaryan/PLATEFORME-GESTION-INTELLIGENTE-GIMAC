package cm.gimac.des.certification.repository;

import cm.gimac.des.certification.domain.CoverageSnapshot;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the CoverageSnapshot entity.
 */
@SuppressWarnings("unused")
@Repository
public interface CoverageSnapshotRepository extends JpaRepository<CoverageSnapshot, UUID>, JpaSpecificationExecutor<CoverageSnapshot> {}
