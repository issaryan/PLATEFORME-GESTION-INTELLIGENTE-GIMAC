package cm.gimac.des.audit.repository;

import cm.gimac.des.audit.domain.ConsistencyProof;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the ConsistencyProof entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ConsistencyProofRepository extends JpaRepository<ConsistencyProof, UUID> {}
