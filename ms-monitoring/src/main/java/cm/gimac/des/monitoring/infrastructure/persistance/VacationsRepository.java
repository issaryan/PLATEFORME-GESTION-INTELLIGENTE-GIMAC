package cm.gimac.des.monitoring.infrastructure.persistance;

import cm.gimac.des.monitoring.domain.Shift;
import cm.gimac.des.monitoring.domain.enumeration.ShiftSlot;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Vacations instanciées depuis la grille publiée (MON-13). */
public interface VacationsRepository extends JpaRepository<Shift, UUID> {
    /** Vacation qui contient l'instant. */
    Optional<Shift> findFirstByStartsAtLessThanEqualAndEndsAtGreaterThanOrderByStartsAtDesc(Instant debut, Instant fin);

    Optional<Shift> findFirstByShiftDateAndSlot(LocalDate jour, ShiftSlot plage);

    Optional<Shift> findByReference(String reference);

    List<Shift> findByShiftDateBetweenOrderByStartsAtAsc(LocalDate du, LocalDate au);
}
