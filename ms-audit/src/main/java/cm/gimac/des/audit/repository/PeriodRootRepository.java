package cm.gimac.des.audit.repository;

import cm.gimac.des.audit.domain.PeriodRoot;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the PeriodRoot entity.
 */
@Repository
public interface PeriodRootRepository extends JpaRepository<PeriodRoot, UUID>, JpaSpecificationExecutor<PeriodRoot> {
    default Optional<PeriodRoot> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<PeriodRoot> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<PeriodRoot> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select periodRoot from PeriodRoot periodRoot left join fetch periodRoot.previousRoot",
        countQuery = "select count(periodRoot) from PeriodRoot periodRoot"
    )
    Page<PeriodRoot> findAllWithToOneRelationships(Pageable pageable);

    @Query("select periodRoot from PeriodRoot periodRoot left join fetch periodRoot.previousRoot")
    List<PeriodRoot> findAllWithToOneRelationships();

    @Query("select periodRoot from PeriodRoot periodRoot left join fetch periodRoot.previousRoot where periodRoot.id =:id")
    Optional<PeriodRoot> findOneWithToOneRelationships(@Param("id") UUID id);
}
