package cm.gimac.des.monitoring.infrastructure.persistance;

import cm.gimac.des.monitoring.domain.OnCallAssignment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Affectations d'une révision de grille. */
public interface AffectationsRepository extends JpaRepository<OnCallAssignment, UUID> {
    List<OnCallAssignment> findByRosterIdOrderByDutyDateAscSlotAscLevelAsc(UUID grille);
}
