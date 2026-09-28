package cm.gimac.des.reporting.repository;

import cm.gimac.des.reporting.domain.Dimension;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Dimension entity.
 */
@SuppressWarnings("unused")
@Repository
public interface DimensionRepository extends JpaRepository<Dimension, UUID>, JpaSpecificationExecutor<Dimension> {}
