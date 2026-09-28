package cm.gimac.des.pilotage.repository;

import cm.gimac.des.pilotage.domain.AbsenceProjection;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the AbsenceProjection entity.
 */
@SuppressWarnings("unused")
@Repository
public interface AbsenceProjectionRepository extends JpaRepository<AbsenceProjection, UUID>, JpaSpecificationExecutor<AbsenceProjection> {}
