package cm.gimac.des.rapprochement.repository;

import cm.gimac.des.rapprochement.domain.ConciliationEntry;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the ConciliationEntry entity.
 */
@Repository
public interface ConciliationEntryRepository extends JpaRepository<ConciliationEntry, UUID> {
    default Optional<ConciliationEntry> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<ConciliationEntry> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<ConciliationEntry> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select conciliationEntry from ConciliationEntry conciliationEntry left join fetch conciliationEntry.suspens",
        countQuery = "select count(conciliationEntry) from ConciliationEntry conciliationEntry"
    )
    Page<ConciliationEntry> findAllWithToOneRelationships(Pageable pageable);

    @Query("select conciliationEntry from ConciliationEntry conciliationEntry left join fetch conciliationEntry.suspens")
    List<ConciliationEntry> findAllWithToOneRelationships();

    @Query(
        "select conciliationEntry from ConciliationEntry conciliationEntry left join fetch conciliationEntry.suspens where conciliationEntry.id =:id"
    )
    Optional<ConciliationEntry> findOneWithToOneRelationships(@Param("id") UUID id);
}
