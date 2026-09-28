package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.ContractedScope;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the ContractedScope entity.
 */
@Repository
public interface ContractedScopeRepository extends JpaRepository<ContractedScope, UUID>, JpaSpecificationExecutor<ContractedScope> {
    default Optional<ContractedScope> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<ContractedScope> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<ContractedScope> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select contractedScope from ContractedScope contractedScope left join fetch contractedScope.liaison",
        countQuery = "select count(contractedScope) from ContractedScope contractedScope"
    )
    Page<ContractedScope> findAllWithToOneRelationships(Pageable pageable);

    @Query("select contractedScope from ContractedScope contractedScope left join fetch contractedScope.liaison")
    List<ContractedScope> findAllWithToOneRelationships();

    @Query(
        "select contractedScope from ContractedScope contractedScope left join fetch contractedScope.liaison where contractedScope.id =:id"
    )
    Optional<ContractedScope> findOneWithToOneRelationships(@Param("id") UUID id);
}
