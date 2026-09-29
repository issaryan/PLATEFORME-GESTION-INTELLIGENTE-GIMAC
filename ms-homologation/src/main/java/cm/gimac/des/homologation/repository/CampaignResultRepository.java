package cm.gimac.des.homologation.repository;

import cm.gimac.des.homologation.domain.CampaignResult;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the CampaignResult entity.
 */
@SuppressWarnings("unused")
@Repository
public interface CampaignResultRepository extends JpaRepository<CampaignResult, UUID> {}
