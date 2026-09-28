package cm.gimac.des.incident.repository;

import cm.gimac.des.incident.domain.ServiceCommitment;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the ServiceCommitment entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ServiceCommitmentRepository extends JpaRepository<ServiceCommitment, UUID> {}
