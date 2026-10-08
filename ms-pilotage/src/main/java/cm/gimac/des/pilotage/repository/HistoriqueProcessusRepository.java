package cm.gimac.des.pilotage.repository;

import cm.gimac.des.pilotage.domain.processus.HistoriqueProcessus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Historique des processus et des instances, en ajout seul (D-87). */
public interface HistoriqueProcessusRepository extends JpaRepository<HistoriqueProcessus, UUID> {
    List<HistoriqueProcessus> findByInstanceIdOrderByQuandAsc(UUID instanceId);

    /** Une étape a-t-elle reçu cette action (réaffectation par le chef d'unité, respectée ensuite, D-93) ? */
    boolean existsByInstanceIdAndEtapeAndAction(UUID instanceId, String etape, cm.gimac.des.pilotage.domain.processus.ActionProcessus action);

    List<HistoriqueProcessus> findByProcessusIdAndInstanceIdIsNullOrderByQuandAsc(UUID processusId);
}
