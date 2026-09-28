package cm.gimac.des.incident.repository;

import cm.gimac.des.incident.domain.CrisisCell;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the CrisisCell entity.
 */
@SuppressWarnings("unused")
@Repository
public interface CrisisCellRepository extends JpaRepository<CrisisCell, UUID>, JpaSpecificationExecutor<CrisisCell> {}
