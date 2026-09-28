package cm.gimac.des.incident.repository;

import cm.gimac.des.incident.domain.AlertObservation;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the AlertObservation entity.
 */
@SuppressWarnings("unused")
@Repository
public interface AlertObservationRepository extends JpaRepository<AlertObservation, UUID> {}
