package cm.gimac.des.monitoring.infrastructure.persistance;

import cm.gimac.des.monitoring.domain.OperatorUnavailability;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Projection des absences et congés validés de MS-IAM (BF-ADMIN). */
public interface IndisponibilitesRepository extends JpaRepository<OperatorUnavailability, UUID> {
    Optional<OperatorUnavailability> findByAvailabilityId(UUID disponibilite);

    List<OperatorUnavailability> findByCancelledFalseAndStartsAtLessThanAndEndsAtGreaterThan(Instant fin, Instant debut);

    List<OperatorUnavailability> findByStartsAtLessThanAndEndsAtGreaterThan(Instant fin, Instant debut);
}
