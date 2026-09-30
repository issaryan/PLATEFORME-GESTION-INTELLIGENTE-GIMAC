package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.parcours.OperationParcours;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Opérations instanciées du parcours. */
@Repository
public interface OperationParcoursRepository extends JpaRepository<OperationParcours, UUID> {
    List<OperationParcours> findByLiaisonIdOrderByOrdre(UUID liaisonId);

    List<OperationParcours> findByLiaisonIdIn(Collection<UUID> liaisonIds);

    /** Liaison d'une opération, lue sans charger l'opération (elle l'est ensuite sous le verrou de la liaison). */
    @Query("select o.liaisonId from OperationParcours o where o.id = :id")
    java.util.Optional<UUID> liaisonDe(@Param("id") UUID id);
}
