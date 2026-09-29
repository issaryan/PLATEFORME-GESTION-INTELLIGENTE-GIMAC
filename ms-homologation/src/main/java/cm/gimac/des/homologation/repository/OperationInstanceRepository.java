package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.OperationInstance;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the OperationInstance entity.
 */
@SuppressWarnings("unused")
@Repository
public interface OperationInstanceRepository extends JpaRepository<OperationInstance, UUID> {}
