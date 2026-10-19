package cm.gimac.des.monitoring.infrastructure.persistance;

import cm.gimac.des.monitoring.domain.ShiftReport;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Rapports de quart (MON-15). */
public interface RapportsQuartRepository extends JpaRepository<ShiftReport, UUID> {
    Optional<ShiftReport> findByShiftId(UUID vacation);
}
