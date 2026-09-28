package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.Operation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Operation entity.
 */
@Repository
public interface OperationRepository extends JpaRepository<Operation, UUID>, JpaSpecificationExecutor<Operation> {
    default Optional<Operation> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Operation> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Operation> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select operation from Operation operation left join fetch operation.liaison left join fetch operation.reference",
        countQuery = "select count(operation) from Operation operation"
    )
    Page<Operation> findAllWithToOneRelationships(Pageable pageable);

    @Query("select operation from Operation operation left join fetch operation.liaison left join fetch operation.reference")
    List<Operation> findAllWithToOneRelationships();

    @Query(
        "select operation from Operation operation left join fetch operation.liaison left join fetch operation.reference where operation.id =:id"
    )
    Optional<Operation> findOneWithToOneRelationships(@Param("id") UUID id);
}
