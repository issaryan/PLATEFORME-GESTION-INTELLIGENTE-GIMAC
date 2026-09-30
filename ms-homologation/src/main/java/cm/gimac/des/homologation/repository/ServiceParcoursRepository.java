package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.parcours.ServiceParcours;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Services du périmètre suivis par le parcours. */
@Repository
public interface ServiceParcoursRepository extends JpaRepository<ServiceParcours, UUID> {
    List<ServiceParcours> findByLiaisonIdOrderByServiceCode(UUID liaisonId);

    List<ServiceParcours> findByLiaisonIdIn(Collection<UUID> liaisonIds);
}
