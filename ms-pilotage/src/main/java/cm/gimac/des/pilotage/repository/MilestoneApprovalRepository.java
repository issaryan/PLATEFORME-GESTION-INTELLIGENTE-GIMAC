package cm.gimac.des.pilotage.repository;

import cm.gimac.des.pilotage.domain.MilestoneApproval;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the MilestoneApproval entity.
 */
@Repository
public interface MilestoneApprovalRepository extends JpaRepository<MilestoneApproval, UUID> {
    default Optional<MilestoneApproval> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<MilestoneApproval> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<MilestoneApproval> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select milestoneApproval from MilestoneApproval milestoneApproval left join fetch milestoneApproval.milestone",
        countQuery = "select count(milestoneApproval) from MilestoneApproval milestoneApproval"
    )
    Page<MilestoneApproval> findAllWithToOneRelationships(Pageable pageable);

    @Query("select milestoneApproval from MilestoneApproval milestoneApproval left join fetch milestoneApproval.milestone")
    List<MilestoneApproval> findAllWithToOneRelationships();

    @Query(
        "select milestoneApproval from MilestoneApproval milestoneApproval left join fetch milestoneApproval.milestone where milestoneApproval.id =:id"
    )
    Optional<MilestoneApproval> findOneWithToOneRelationships(@Param("id") UUID id);
}
