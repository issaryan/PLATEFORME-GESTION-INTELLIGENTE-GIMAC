package cm.gimac.des.pilotage.repository;

import cm.gimac.des.pilotage.domain.activite.TacheHistorique;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Historique des tâches, en ajout seul (§2.3) : aucune mise à jour ni suppression n'est faite. */
public interface TacheHistoriqueRepository extends JpaRepository<TacheHistorique, UUID> {
    List<TacheHistorique> findByTacheIdOrderByQuandAsc(UUID tacheId);
}
