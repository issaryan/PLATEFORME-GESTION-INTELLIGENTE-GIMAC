package cm.gimac.des.iam.repository;

import cm.gimac.des.iam.domain.AccessReviewDecision;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the AccessReviewDecision entity.
 */
@Repository
public interface AccessReviewDecisionRepository extends JpaRepository<AccessReviewDecision, UUID> {
    default Optional<AccessReviewDecision> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<AccessReviewDecision> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<AccessReviewDecision> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select accessReviewDecision from AccessReviewDecision accessReviewDecision left join fetch accessReviewDecision.review left join fetch accessReviewDecision.collaborator",
        countQuery = "select count(accessReviewDecision) from AccessReviewDecision accessReviewDecision"
    )
    Page<AccessReviewDecision> findAllWithToOneRelationships(Pageable pageable);

    @Query(
        "select accessReviewDecision from AccessReviewDecision accessReviewDecision left join fetch accessReviewDecision.review left join fetch accessReviewDecision.collaborator"
    )
    List<AccessReviewDecision> findAllWithToOneRelationships();

    @Query(
        "select accessReviewDecision from AccessReviewDecision accessReviewDecision left join fetch accessReviewDecision.review left join fetch accessReviewDecision.collaborator where accessReviewDecision.id =:id"
    )
    Optional<AccessReviewDecision> findOneWithToOneRelationships(@Param("id") UUID id);
}
