package cm.gimac.des.monitoring.repository;

import cm.gimac.des.monitoring.domain.Alert;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Alert entity.
 */
@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID>, JpaSpecificationExecutor<Alert> {
    default Optional<Alert> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Alert> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Alert> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select alert from Alert alert left join fetch alert.link left join fetch alert.correlationRoot",
        countQuery = "select count(alert) from Alert alert"
    )
    Page<Alert> findAllWithToOneRelationships(Pageable pageable);

    @Query("select alert from Alert alert left join fetch alert.link left join fetch alert.correlationRoot")
    List<Alert> findAllWithToOneRelationships();

    @Query("select alert from Alert alert left join fetch alert.link left join fetch alert.correlationRoot where alert.id =:id")
    Optional<Alert> findOneWithToOneRelationships(@Param("id") UUID id);
}
