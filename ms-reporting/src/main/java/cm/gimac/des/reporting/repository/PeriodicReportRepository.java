package cm.gimac.des.reporting.repository;

import cm.gimac.des.reporting.domain.PeriodicReport;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the PeriodicReport entity.
 */
@SuppressWarnings("unused")
@Repository
public interface PeriodicReportRepository extends JpaRepository<PeriodicReport, UUID>, JpaSpecificationExecutor<PeriodicReport> {}
