package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.CampaignSync;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the CampaignSync entity.
 */
@Repository
public interface CampaignSyncRepository extends JpaRepository<CampaignSync, UUID> {
    default Optional<CampaignSync> findOneWithEagerRelationships(UUID id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<CampaignSync> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<CampaignSync> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select campaignSync from CampaignSync campaignSync left join fetch campaignSync.liaison",
        countQuery = "select count(campaignSync) from CampaignSync campaignSync"
    )
    Page<CampaignSync> findAllWithToOneRelationships(Pageable pageable);

    @Query("select campaignSync from CampaignSync campaignSync left join fetch campaignSync.liaison")
    List<CampaignSync> findAllWithToOneRelationships();

    @Query("select campaignSync from CampaignSync campaignSync left join fetch campaignSync.liaison where campaignSync.id =:id")
    Optional<CampaignSync> findOneWithToOneRelationships(@Param("id") UUID id);
}
