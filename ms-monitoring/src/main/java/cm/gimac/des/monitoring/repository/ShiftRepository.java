package cm.gimac.des.monitoring.repository;

import cm.gimac.des.monitoring.domain.Shift;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Shift entity.
 */
@Repository
public interface ShiftRepository extends JpaRepository<Shift, UUID>, JpaSpecificationExecutor<Shift> {
    default Optional<Shift> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Shift> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Shift> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(value = "select shift from Shift shift left join fetch shift.roster", countQuery = "select count(shift) from Shift shift")
    Page<Shift> findAllWithToOneRelationships(Pageable pageable);

    @Query("select shift from Shift shift left join fetch shift.roster")
    List<Shift> findAllWithToOneRelationships();

    @Query("select shift from Shift shift left join fetch shift.roster where shift.id =:id")
    Optional<Shift> findOneWithToOneRelationships(@Param("id") UUID id);
}
