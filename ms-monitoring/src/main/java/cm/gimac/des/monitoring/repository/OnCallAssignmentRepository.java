package cm.gimac.des.monitoring.repository;

import cm.gimac.des.monitoring.domain.OnCallAssignment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the OnCallAssignment entity.
 */
@Repository
public interface OnCallAssignmentRepository extends JpaRepository<OnCallAssignment, UUID>, JpaSpecificationExecutor<OnCallAssignment> {
    default Optional<OnCallAssignment> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<OnCallAssignment> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<OnCallAssignment> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select onCallAssignment from OnCallAssignment onCallAssignment left join fetch onCallAssignment.roster",
        countQuery = "select count(onCallAssignment) from OnCallAssignment onCallAssignment"
    )
    Page<OnCallAssignment> findAllWithToOneRelationships(Pageable pageable);

    @Query("select onCallAssignment from OnCallAssignment onCallAssignment left join fetch onCallAssignment.roster")
    List<OnCallAssignment> findAllWithToOneRelationships();

    @Query(
        "select onCallAssignment from OnCallAssignment onCallAssignment left join fetch onCallAssignment.roster where onCallAssignment.id =:id"
    )
    Optional<OnCallAssignment> findOneWithToOneRelationships(@Param("id") UUID id);
}
