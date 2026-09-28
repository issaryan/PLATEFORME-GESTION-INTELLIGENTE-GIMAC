package cm.gimac.des.pilotage.repository;

import cm.gimac.des.pilotage.domain.CollaboratorProjection;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the CollaboratorProjection entity.
 */
@SuppressWarnings("unused")
@Repository
public interface CollaboratorProjectionRepository
    extends JpaRepository<CollaboratorProjection, UUID>, JpaSpecificationExecutor<CollaboratorProjection> {}
