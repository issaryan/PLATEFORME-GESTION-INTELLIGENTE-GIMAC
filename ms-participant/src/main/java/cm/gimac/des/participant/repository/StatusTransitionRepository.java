package cm.gimac.des.participant.repository;

import cm.gimac.des.participant.domain.StatusTransition;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the StatusTransition entity.
 */
@Repository
public interface StatusTransitionRepository extends JpaRepository<StatusTransition, UUID>, JpaSpecificationExecutor<StatusTransition> {
    default Optional<StatusTransition> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<StatusTransition> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<StatusTransition> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select statusTransition from StatusTransition statusTransition left join fetch statusTransition.institution",
        countQuery = "select count(statusTransition) from StatusTransition statusTransition"
    )
    Page<StatusTransition> findAllWithToOneRelationships(Pageable pageable);

    @Query("select statusTransition from StatusTransition statusTransition left join fetch statusTransition.institution")
    List<StatusTransition> findAllWithToOneRelationships();

    @Query(
        "select statusTransition from StatusTransition statusTransition left join fetch statusTransition.institution where statusTransition.id =:id"
    )
    Optional<StatusTransition> findOneWithToOneRelationships(@Param("id") UUID id);
}
