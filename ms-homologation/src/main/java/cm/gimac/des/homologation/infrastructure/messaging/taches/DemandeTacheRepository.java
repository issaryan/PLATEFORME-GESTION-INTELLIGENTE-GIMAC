package cm.gimac.des.homologation.infrastructure.messaging.taches;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DemandeTacheRepository extends JpaRepository<DemandeTache, Long> {
    /** Demandes en attente dans l'ordre d'écriture, verrouillées par lot (plusieurs réplicas sans doublon). */
    @Query(value = "SELECT * FROM demande_tache_pilotage WHERE statut = 'EN_ATTENTE' ORDER BY numero LIMIT :n FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<DemandeTache> verrouillerEnAttente(@Param("n") int n);

    List<DemandeTache> findByCleOrderByNumero(String cle);

    List<DemandeTache> findAllByOrderByNumero();
}
