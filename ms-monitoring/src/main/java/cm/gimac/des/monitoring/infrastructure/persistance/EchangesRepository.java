package cm.gimac.des.monitoring.infrastructure.persistance;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Échanges d'astreinte (MON-12). */
public interface EchangesRepository extends JpaRepository<EchangeAstreinte, UUID> {
    List<EchangeAstreinte> findByGrilleIdOrderByCreeLeDesc(UUID grille);

    List<EchangeAstreinte> findByGrilleIdAndEtatIn(UUID grille, List<EchangeAstreinte.Etat> etats);
}
