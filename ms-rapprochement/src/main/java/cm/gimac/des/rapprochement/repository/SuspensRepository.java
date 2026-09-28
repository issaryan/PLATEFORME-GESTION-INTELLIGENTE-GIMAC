package cm.gimac.des.rapprochement.repository;

import cm.gimac.des.rapprochement.domain.Suspens;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Suspens entity.
 */
@Repository
public interface SuspensRepository extends JpaRepository<Suspens, UUID>, JpaSpecificationExecutor<Suspens> {
    default Optional<Suspens> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Suspens> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Suspens> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select suspens from Suspens suspens left join fetch suspens.reconciliation",
        countQuery = "select count(suspens) from Suspens suspens"
    )
    Page<Suspens> findAllWithToOneRelationships(Pageable pageable);

    @Query("select suspens from Suspens suspens left join fetch suspens.reconciliation")
    List<Suspens> findAllWithToOneRelationships();

    @Query("select suspens from Suspens suspens left join fetch suspens.reconciliation where suspens.id =:id")
    Optional<Suspens> findOneWithToOneRelationships(@Param("id") UUID id);
}
