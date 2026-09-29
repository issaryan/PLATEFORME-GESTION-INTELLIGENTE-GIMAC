package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.EvidenceFileExport;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the EvidenceFileExport entity.
 */
@SuppressWarnings("unused")
@Repository
public interface EvidenceFileExportRepository extends JpaRepository<EvidenceFileExport, UUID> {}
