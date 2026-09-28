package cm.gimac.des.document.repository;

import cm.gimac.des.document.domain.PurgeRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the PurgeRequest entity.
 */
@Repository
public interface PurgeRequestRepository extends JpaRepository<PurgeRequest, UUID>, JpaSpecificationExecutor<PurgeRequest> {
    default Optional<PurgeRequest> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<PurgeRequest> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<PurgeRequest> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select purgeRequest from PurgeRequest purgeRequest left join fetch purgeRequest.document",
        countQuery = "select count(purgeRequest) from PurgeRequest purgeRequest"
    )
    Page<PurgeRequest> findAllWithToOneRelationships(Pageable pageable);

    @Query("select purgeRequest from PurgeRequest purgeRequest left join fetch purgeRequest.document")
    List<PurgeRequest> findAllWithToOneRelationships();

    @Query("select purgeRequest from PurgeRequest purgeRequest left join fetch purgeRequest.document where purgeRequest.id =:id")
    Optional<PurgeRequest> findOneWithToOneRelationships(@Param("id") UUID id);
}
