package cm.gimac.des.pilotage.repository;

import cm.gimac.des.pilotage.domain.Milestone;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Milestone entity.
 *
 * When extending this class, extend MilestoneRepositoryWithBagRelationships too.
 * For more information refer to https://github.com/jhipster/generator-jhipster/issues/17990.
 */
@Repository
public interface MilestoneRepository
    extends MilestoneRepositoryWithBagRelationships, JpaRepository<Milestone, UUID>, JpaSpecificationExecutor<Milestone> {
    default Optional<Milestone> findOneWithEagerRelationships(UUID id) {
        return this.fetchBagRelationships(this.findOneWithToOneRelationships(id));
    }

    default List<Milestone> findAllWithEagerRelationships() {
        return this.fetchBagRelationships(this.findAllWithToOneRelationships());
    }

    default Page<Milestone> findAllWithEagerRelationships(Pageable pageable) {
        return this.fetchBagRelationships(this.findAllWithToOneRelationships(pageable));
    }

    @Query(
        value = "select milestone from Milestone milestone left join fetch milestone.project",
        countQuery = "select count(milestone) from Milestone milestone"
    )
    Page<Milestone> findAllWithToOneRelationships(Pageable pageable);

    @Query("select milestone from Milestone milestone left join fetch milestone.project")
    List<Milestone> findAllWithToOneRelationships();

    @Query("select milestone from Milestone milestone left join fetch milestone.project where milestone.id =:id")
    Optional<Milestone> findOneWithToOneRelationships(@Param("id") UUID id);
}
