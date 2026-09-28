package cm.gimac.des.document.repository;

import cm.gimac.des.document.domain.IntegrityMismatch;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the IntegrityMismatch entity.
 */
@Repository
public interface IntegrityMismatchRepository extends JpaRepository<IntegrityMismatch, UUID> {
    default Optional<IntegrityMismatch> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<IntegrityMismatch> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<IntegrityMismatch> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select integrityMismatch from IntegrityMismatch integrityMismatch left join fetch integrityMismatch.document",
        countQuery = "select count(integrityMismatch) from IntegrityMismatch integrityMismatch"
    )
    Page<IntegrityMismatch> findAllWithToOneRelationships(Pageable pageable);

    @Query("select integrityMismatch from IntegrityMismatch integrityMismatch left join fetch integrityMismatch.document")
    List<IntegrityMismatch> findAllWithToOneRelationships();

    @Query(
        "select integrityMismatch from IntegrityMismatch integrityMismatch left join fetch integrityMismatch.document where integrityMismatch.id =:id"
    )
    Optional<IntegrityMismatch> findOneWithToOneRelationships(@Param("id") UUID id);
}
