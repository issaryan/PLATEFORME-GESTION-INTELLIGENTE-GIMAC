package cm.gimac.des.monitoring.repository;

import cm.gimac.des.monitoring.domain.ShiftLogEntry;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the ShiftLogEntry entity.
 */
@Repository
public interface ShiftLogEntryRepository extends JpaRepository<ShiftLogEntry, UUID>, JpaSpecificationExecutor<ShiftLogEntry> {
    default Optional<ShiftLogEntry> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<ShiftLogEntry> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<ShiftLogEntry> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select shiftLogEntry from ShiftLogEntry shiftLogEntry left join fetch shiftLogEntry.shift",
        countQuery = "select count(shiftLogEntry) from ShiftLogEntry shiftLogEntry"
    )
    Page<ShiftLogEntry> findAllWithToOneRelationships(Pageable pageable);

    @Query("select shiftLogEntry from ShiftLogEntry shiftLogEntry left join fetch shiftLogEntry.shift")
    List<ShiftLogEntry> findAllWithToOneRelationships();

    @Query("select shiftLogEntry from ShiftLogEntry shiftLogEntry left join fetch shiftLogEntry.shift where shiftLogEntry.id =:id")
    Optional<ShiftLogEntry> findOneWithToOneRelationships(@Param("id") UUID id);
}
