package cm.gimac.des.incident.repository;

import cm.gimac.des.incident.domain.Incident;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Incident entity.
 */
@Repository
public interface IncidentRepository extends JpaRepository<Incident, UUID>, JpaSpecificationExecutor<Incident> {
    default Optional<Incident> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Incident> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Incident> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select incident from Incident incident left join fetch incident.ticket left join fetch incident.matrix",
        countQuery = "select count(incident) from Incident incident"
    )
    Page<Incident> findAllWithToOneRelationships(Pageable pageable);

    @Query("select incident from Incident incident left join fetch incident.ticket left join fetch incident.matrix")
    List<Incident> findAllWithToOneRelationships();

    @Query("select incident from Incident incident left join fetch incident.ticket left join fetch incident.matrix where incident.id =:id")
    Optional<Incident> findOneWithToOneRelationships(@Param("id") UUID id);
}
