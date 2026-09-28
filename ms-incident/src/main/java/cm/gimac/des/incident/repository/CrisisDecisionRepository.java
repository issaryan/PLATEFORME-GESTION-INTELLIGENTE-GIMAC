package cm.gimac.des.incident.repository;

import cm.gimac.des.incident.domain.CrisisDecision;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the CrisisDecision entity.
 */
@Repository
public interface CrisisDecisionRepository extends JpaRepository<CrisisDecision, UUID> {
    default Optional<CrisisDecision> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<CrisisDecision> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<CrisisDecision> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select crisisDecision from CrisisDecision crisisDecision left join fetch crisisDecision.cell",
        countQuery = "select count(crisisDecision) from CrisisDecision crisisDecision"
    )
    Page<CrisisDecision> findAllWithToOneRelationships(Pageable pageable);

    @Query("select crisisDecision from CrisisDecision crisisDecision left join fetch crisisDecision.cell")
    List<CrisisDecision> findAllWithToOneRelationships();

    @Query("select crisisDecision from CrisisDecision crisisDecision left join fetch crisisDecision.cell where crisisDecision.id =:id")
    Optional<CrisisDecision> findOneWithToOneRelationships(@Param("id") UUID id);
}
