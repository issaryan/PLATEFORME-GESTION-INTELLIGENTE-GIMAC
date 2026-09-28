package cm.gimac.des.pilotage.repository;

import cm.gimac.des.pilotage.domain.Decision;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Decision entity.
 */
@Repository
public interface DecisionRepository extends JpaRepository<Decision, UUID>, JpaSpecificationExecutor<Decision> {
    default Optional<Decision> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Decision> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Decision> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select decision from Decision decision left join fetch decision.task left join fetch decision.meeting left join fetch decision.rectifies",
        countQuery = "select count(decision) from Decision decision"
    )
    Page<Decision> findAllWithToOneRelationships(Pageable pageable);

    @Query(
        "select decision from Decision decision left join fetch decision.task left join fetch decision.meeting left join fetch decision.rectifies"
    )
    List<Decision> findAllWithToOneRelationships();

    @Query(
        "select decision from Decision decision left join fetch decision.task left join fetch decision.meeting left join fetch decision.rectifies where decision.id =:id"
    )
    Optional<Decision> findOneWithToOneRelationships(@Param("id") UUID id);
}
