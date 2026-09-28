package cm.gimac.des.pilotage.repository;

import cm.gimac.des.pilotage.domain.Risk;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Risk entity.
 */
@Repository
public interface RiskRepository extends JpaRepository<Risk, UUID>, JpaSpecificationExecutor<Risk> {
    default Optional<Risk> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Risk> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Risk> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(value = "select risk from Risk risk left join fetch risk.project", countQuery = "select count(risk) from Risk risk")
    Page<Risk> findAllWithToOneRelationships(Pageable pageable);

    @Query("select risk from Risk risk left join fetch risk.project")
    List<Risk> findAllWithToOneRelationships();

    @Query("select risk from Risk risk left join fetch risk.project where risk.id =:id")
    Optional<Risk> findOneWithToOneRelationships(@Param("id") UUID id);
}
