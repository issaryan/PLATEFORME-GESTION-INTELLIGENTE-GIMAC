package cm.gimac.des.monitoring.repository;

import cm.gimac.des.monitoring.domain.AlertSource;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the AlertSource entity.
 */
@SuppressWarnings("unused")
@Repository
public interface AlertSourceRepository extends JpaRepository<AlertSource, UUID> {}
