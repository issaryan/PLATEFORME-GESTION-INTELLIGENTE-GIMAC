package cm.gimac.des.monitoring.repository;

import cm.gimac.des.monitoring.domain.OnCallRoster;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the OnCallRoster entity.
 */
@Repository
public interface OnCallRosterRepository extends JpaRepository<OnCallRoster, UUID> {
    default Optional<OnCallRoster> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<OnCallRoster> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<OnCallRoster> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select onCallRoster from OnCallRoster onCallRoster left join fetch onCallRoster.previousRevision",
        countQuery = "select count(onCallRoster) from OnCallRoster onCallRoster"
    )
    Page<OnCallRoster> findAllWithToOneRelationships(Pageable pageable);

    @Query("select onCallRoster from OnCallRoster onCallRoster left join fetch onCallRoster.previousRevision")
    List<OnCallRoster> findAllWithToOneRelationships();

    @Query("select onCallRoster from OnCallRoster onCallRoster left join fetch onCallRoster.previousRevision where onCallRoster.id =:id")
    Optional<OnCallRoster> findOneWithToOneRelationships(@Param("id") UUID id);
}
