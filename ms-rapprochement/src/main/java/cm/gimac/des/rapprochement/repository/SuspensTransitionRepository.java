package cm.gimac.des.rapprochement.repository;

import cm.gimac.des.rapprochement.domain.SuspensTransition;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the SuspensTransition entity.
 */
@Repository
public interface SuspensTransitionRepository extends JpaRepository<SuspensTransition, UUID>, JpaSpecificationExecutor<SuspensTransition> {
    default Optional<SuspensTransition> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<SuspensTransition> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<SuspensTransition> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select suspensTransition from SuspensTransition suspensTransition left join fetch suspensTransition.suspens",
        countQuery = "select count(suspensTransition) from SuspensTransition suspensTransition"
    )
    Page<SuspensTransition> findAllWithToOneRelationships(Pageable pageable);

    @Query("select suspensTransition from SuspensTransition suspensTransition left join fetch suspensTransition.suspens")
    List<SuspensTransition> findAllWithToOneRelationships();

    @Query(
        "select suspensTransition from SuspensTransition suspensTransition left join fetch suspensTransition.suspens where suspensTransition.id =:id"
    )
    Optional<SuspensTransition> findOneWithToOneRelationships(@Param("id") UUID id);
}
