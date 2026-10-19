package cm.gimac.des.monitoring.infrastructure.persistance;

import cm.gimac.des.monitoring.domain.OnCallRoster;
import cm.gimac.des.monitoring.domain.enumeration.RosterState;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Révisions de la grille d'astreinte d'une semaine (MON-09 à MON-12). */
public interface GrillesRepository extends JpaRepository<OnCallRoster, UUID> {
    List<OnCallRoster> findByIsoWeekOrderByRevisionDesc(String semaine);

    Optional<OnCallRoster> findFirstByIsoWeekAndStateOrderByRevisionDesc(String semaine, RosterState etat);

    Optional<OnCallRoster> findFirstByIsoWeekOrderByRevisionDesc(String semaine);
}
