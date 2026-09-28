package cm.gimac.des.participant.repository;

import cm.gimac.des.participant.domain.Convention;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Convention entity.
 */
@Repository
public interface ConventionRepository extends JpaRepository<Convention, UUID>, JpaSpecificationExecutor<Convention> {
    default Optional<Convention> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Convention> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Convention> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select convention from Convention convention left join fetch convention.institution",
        countQuery = "select count(convention) from Convention convention"
    )
    Page<Convention> findAllWithToOneRelationships(Pageable pageable);

    @Query("select convention from Convention convention left join fetch convention.institution")
    List<Convention> findAllWithToOneRelationships();

    @Query("select convention from Convention convention left join fetch convention.institution where convention.id =:id")
    Optional<Convention> findOneWithToOneRelationships(@Param("id") UUID id);
}
