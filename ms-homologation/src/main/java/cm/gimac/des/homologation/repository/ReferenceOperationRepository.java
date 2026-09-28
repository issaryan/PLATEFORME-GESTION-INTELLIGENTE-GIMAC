package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.ReferenceOperation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the ReferenceOperation entity.
 *
 * When extending this class, extend ReferenceOperationRepositoryWithBagRelationships too.
 * For more information refer to https://github.com/jhipster/generator-jhipster/issues/17990.
 */
@Repository
public interface ReferenceOperationRepository
    extends
        ReferenceOperationRepositoryWithBagRelationships,
        JpaRepository<ReferenceOperation, UUID>,
        JpaSpecificationExecutor<ReferenceOperation> {
    default Optional<ReferenceOperation> findOneWithEagerRelationships(UUID id) {
        return this.fetchBagRelationships(this.findOneWithToOneRelationships(id));
    }

    default List<ReferenceOperation> findAllWithEagerRelationships() {
        return this.fetchBagRelationships(this.findAllWithToOneRelationships());
    }

    default Page<ReferenceOperation> findAllWithEagerRelationships(Pageable pageable) {
        return this.fetchBagRelationships(this.findAllWithToOneRelationships(pageable));
    }

    @Query(
        value = "select referenceOperation from ReferenceOperation referenceOperation left join fetch referenceOperation.catalog",
        countQuery = "select count(referenceOperation) from ReferenceOperation referenceOperation"
    )
    Page<ReferenceOperation> findAllWithToOneRelationships(Pageable pageable);

    @Query("select referenceOperation from ReferenceOperation referenceOperation left join fetch referenceOperation.catalog")
    List<ReferenceOperation> findAllWithToOneRelationships();

    @Query(
        "select referenceOperation from ReferenceOperation referenceOperation left join fetch referenceOperation.catalog where referenceOperation.id =:id"
    )
    Optional<ReferenceOperation> findOneWithToOneRelationships(@Param("id") UUID id);
}
