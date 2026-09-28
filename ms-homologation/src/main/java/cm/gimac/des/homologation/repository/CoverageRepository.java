package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.Coverage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Coverage entity.
 */
@Repository
public interface CoverageRepository extends JpaRepository<Coverage, UUID>, JpaSpecificationExecutor<Coverage> {
    default Optional<Coverage> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Coverage> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Coverage> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select coverage from Coverage coverage left join fetch coverage.liaison",
        countQuery = "select count(coverage) from Coverage coverage"
    )
    Page<Coverage> findAllWithToOneRelationships(Pageable pageable);

    @Query("select coverage from Coverage coverage left join fetch coverage.liaison")
    List<Coverage> findAllWithToOneRelationships();

    @Query("select coverage from Coverage coverage left join fetch coverage.liaison where coverage.id =:id")
    Optional<Coverage> findOneWithToOneRelationships(@Param("id") UUID id);
}
