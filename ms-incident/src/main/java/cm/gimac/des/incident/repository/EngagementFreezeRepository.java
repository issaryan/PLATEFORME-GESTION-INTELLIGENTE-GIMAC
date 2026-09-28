package cm.gimac.des.incident.repository;

import cm.gimac.des.incident.domain.EngagementFreeze;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the EngagementFreeze entity.
 */
@Repository
public interface EngagementFreezeRepository extends JpaRepository<EngagementFreeze, UUID> {
    default Optional<EngagementFreeze> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<EngagementFreeze> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<EngagementFreeze> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select engagementFreeze from EngagementFreeze engagementFreeze left join fetch engagementFreeze.ticket",
        countQuery = "select count(engagementFreeze) from EngagementFreeze engagementFreeze"
    )
    Page<EngagementFreeze> findAllWithToOneRelationships(Pageable pageable);

    @Query("select engagementFreeze from EngagementFreeze engagementFreeze left join fetch engagementFreeze.ticket")
    List<EngagementFreeze> findAllWithToOneRelationships();

    @Query(
        "select engagementFreeze from EngagementFreeze engagementFreeze left join fetch engagementFreeze.ticket where engagementFreeze.id =:id"
    )
    Optional<EngagementFreeze> findOneWithToOneRelationships(@Param("id") UUID id);
}
