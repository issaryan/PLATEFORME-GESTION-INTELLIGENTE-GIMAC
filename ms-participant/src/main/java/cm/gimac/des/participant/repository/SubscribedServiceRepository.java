package cm.gimac.des.participant.repository;

import cm.gimac.des.participant.domain.SubscribedService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the SubscribedService entity.
 */
@Repository
public interface SubscribedServiceRepository extends JpaRepository<SubscribedService, UUID>, JpaSpecificationExecutor<SubscribedService> {
    default Optional<SubscribedService> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<SubscribedService> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<SubscribedService> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select subscribedService from SubscribedService subscribedService left join fetch subscribedService.institution",
        countQuery = "select count(subscribedService) from SubscribedService subscribedService"
    )
    Page<SubscribedService> findAllWithToOneRelationships(Pageable pageable);

    @Query("select subscribedService from SubscribedService subscribedService left join fetch subscribedService.institution")
    List<SubscribedService> findAllWithToOneRelationships();

    @Query(
        "select subscribedService from SubscribedService subscribedService left join fetch subscribedService.institution where subscribedService.id =:id"
    )
    Optional<SubscribedService> findOneWithToOneRelationships(@Param("id") UUID id);
}
