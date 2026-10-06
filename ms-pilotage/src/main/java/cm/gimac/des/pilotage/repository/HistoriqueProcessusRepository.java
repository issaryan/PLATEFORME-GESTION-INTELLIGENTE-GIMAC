package cm.gimac.des.pilotage.repository;

import cm.gimac.des.pilotage.domain.processus.HistoriqueProcessus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Historique des processus et des instances, en ajout seul (D-87). */
public interface HistoriqueProcessusRepository extends JpaRepository<HistoriqueProcessus, UUID> {
    List<HistoriqueProcessus> findByInstanceIdOrderByQuandAsc(UUID instanceId);

    List<HistoriqueProcessus> findByProcessusIdAndInstanceIdIsNullOrderByQuandAsc(UUID processusId);
}
