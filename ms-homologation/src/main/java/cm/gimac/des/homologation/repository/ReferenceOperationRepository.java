package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.ReferenceOperation;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the ReferenceOperation entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ReferenceOperationRepository extends JpaRepository<ReferenceOperation, UUID> {}
