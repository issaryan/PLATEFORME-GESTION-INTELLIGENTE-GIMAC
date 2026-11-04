package cm.gimac.des.iam.repository;

import cm.gimac.des.iam.domain.Collaborator;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Collaborator entity.
 */
@SuppressWarnings("unused")
@Repository
public interface CollaboratorRepository extends JpaRepository<Collaborator, UUID>, JpaSpecificationExecutor<Collaborator> {}
