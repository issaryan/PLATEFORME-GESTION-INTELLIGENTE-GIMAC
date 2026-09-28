package cm.gimac.des.reporting.repository;

import cm.gimac.des.reporting.domain.DataExport;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the DataExport entity.
 */
@SuppressWarnings("unused")
@Repository
public interface DataExportRepository extends JpaRepository<DataExport, UUID>, JpaSpecificationExecutor<DataExport> {}
