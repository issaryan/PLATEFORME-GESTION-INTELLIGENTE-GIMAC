package cm.gimac.des.rapprochement.repository;

import cm.gimac.des.rapprochement.domain.Reconciliation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Reconciliation entity.
 */
@Repository
public interface ReconciliationRepository extends JpaRepository<Reconciliation, UUID>, JpaSpecificationExecutor<Reconciliation> {
    default Optional<Reconciliation> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Reconciliation> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Reconciliation> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select reconciliation from Reconciliation reconciliation left join fetch reconciliation.switchStatement left join fetch reconciliation.participantStatement left join fetch reconciliation.settlementStatement",
        countQuery = "select count(reconciliation) from Reconciliation reconciliation"
    )
    Page<Reconciliation> findAllWithToOneRelationships(Pageable pageable);

    @Query(
        "select reconciliation from Reconciliation reconciliation left join fetch reconciliation.switchStatement left join fetch reconciliation.participantStatement left join fetch reconciliation.settlementStatement"
    )
    List<Reconciliation> findAllWithToOneRelationships();

    @Query(
        "select reconciliation from Reconciliation reconciliation left join fetch reconciliation.switchStatement left join fetch reconciliation.participantStatement left join fetch reconciliation.settlementStatement where reconciliation.id =:id"
    )
    Optional<Reconciliation> findOneWithToOneRelationships(@Param("id") UUID id);
}
