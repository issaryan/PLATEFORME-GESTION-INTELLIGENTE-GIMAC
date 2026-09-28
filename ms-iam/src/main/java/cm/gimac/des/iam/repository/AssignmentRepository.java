package cm.gimac.des.iam.repository;

import cm.gimac.des.iam.domain.Assignment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Assignment entity.
 */
@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {
    default Optional<Assignment> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Assignment> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Assignment> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select assignment from Assignment assignment left join fetch assignment.collaborator",
        countQuery = "select count(assignment) from Assignment assignment"
    )
    Page<Assignment> findAllWithToOneRelationships(Pageable pageable);

    @Query("select assignment from Assignment assignment left join fetch assignment.collaborator")
    List<Assignment> findAllWithToOneRelationships();

    @Query("select assignment from Assignment assignment left join fetch assignment.collaborator where assignment.id =:id")
    Optional<Assignment> findOneWithToOneRelationships(@Param("id") UUID id);
}
