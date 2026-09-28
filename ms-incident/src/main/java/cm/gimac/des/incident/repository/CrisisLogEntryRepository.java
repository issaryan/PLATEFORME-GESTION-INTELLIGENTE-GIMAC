package cm.gimac.des.incident.repository;

import cm.gimac.des.incident.domain.CrisisLogEntry;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the CrisisLogEntry entity.
 */
@Repository
public interface CrisisLogEntryRepository extends JpaRepository<CrisisLogEntry, UUID>, JpaSpecificationExecutor<CrisisLogEntry> {
    default Optional<CrisisLogEntry> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<CrisisLogEntry> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<CrisisLogEntry> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select crisisLogEntry from CrisisLogEntry crisisLogEntry left join fetch crisisLogEntry.cell",
        countQuery = "select count(crisisLogEntry) from CrisisLogEntry crisisLogEntry"
    )
    Page<CrisisLogEntry> findAllWithToOneRelationships(Pageable pageable);

    @Query("select crisisLogEntry from CrisisLogEntry crisisLogEntry left join fetch crisisLogEntry.cell")
    List<CrisisLogEntry> findAllWithToOneRelationships();

    @Query("select crisisLogEntry from CrisisLogEntry crisisLogEntry left join fetch crisisLogEntry.cell where crisisLogEntry.id =:id")
    Optional<CrisisLogEntry> findOneWithToOneRelationships(@Param("id") UUID id);
}
