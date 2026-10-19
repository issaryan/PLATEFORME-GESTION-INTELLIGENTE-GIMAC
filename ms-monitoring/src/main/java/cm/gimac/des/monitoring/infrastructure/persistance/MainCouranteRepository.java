package cm.gimac.des.monitoring.infrastructure.persistance;

import cm.gimac.des.monitoring.domain.ShiftLogEntry;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Main courante chaînée d'une vacation (MON-14). */
public interface MainCouranteRepository extends JpaRepository<ShiftLogEntry, UUID> {
    Optional<ShiftLogEntry> findFirstByShiftIdOrderByChainIndexDesc(UUID vacation);

    Page<ShiftLogEntry> findByShiftIdOrderByChainIndexDesc(UUID vacation, Pageable page);

    List<ShiftLogEntry> findByShiftIdOrderByChainIndexAsc(UUID vacation);

    long countByShiftId(UUID vacation);
}
