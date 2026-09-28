package cm.gimac.des.pilotage.repository;

import cm.gimac.des.pilotage.domain.AcceptanceCriterion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the AcceptanceCriterion entity.
 */
@Repository
public interface AcceptanceCriterionRepository
    extends JpaRepository<AcceptanceCriterion, UUID>, JpaSpecificationExecutor<AcceptanceCriterion> {
    default Optional<AcceptanceCriterion> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<AcceptanceCriterion> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<AcceptanceCriterion> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select acceptanceCriterion from AcceptanceCriterion acceptanceCriterion left join fetch acceptanceCriterion.task",
        countQuery = "select count(acceptanceCriterion) from AcceptanceCriterion acceptanceCriterion"
    )
    Page<AcceptanceCriterion> findAllWithToOneRelationships(Pageable pageable);

    @Query("select acceptanceCriterion from AcceptanceCriterion acceptanceCriterion left join fetch acceptanceCriterion.task")
    List<AcceptanceCriterion> findAllWithToOneRelationships();

    @Query(
        "select acceptanceCriterion from AcceptanceCriterion acceptanceCriterion left join fetch acceptanceCriterion.task where acceptanceCriterion.id =:id"
    )
    Optional<AcceptanceCriterion> findOneWithToOneRelationships(@Param("id") UUID id);
}
