package cm.gimac.des.participant.repository;

import cm.gimac.des.participant.domain.Deadline;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Deadline entity.
 */
@Repository
public interface DeadlineRepository extends JpaRepository<Deadline, UUID>, JpaSpecificationExecutor<Deadline> {
    default Optional<Deadline> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Deadline> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Deadline> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select deadline from Deadline deadline left join fetch deadline.institution left join fetch deadline.legalDocument left join fetch deadline.convention",
        countQuery = "select count(deadline) from Deadline deadline"
    )
    Page<Deadline> findAllWithToOneRelationships(Pageable pageable);

    @Query(
        "select deadline from Deadline deadline left join fetch deadline.institution left join fetch deadline.legalDocument left join fetch deadline.convention"
    )
    List<Deadline> findAllWithToOneRelationships();

    @Query(
        "select deadline from Deadline deadline left join fetch deadline.institution left join fetch deadline.legalDocument left join fetch deadline.convention where deadline.id =:id"
    )
    Optional<Deadline> findOneWithToOneRelationships(@Param("id") UUID id);
}
