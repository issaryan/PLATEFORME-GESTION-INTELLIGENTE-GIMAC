package cm.gimac.des.homologation.service;

import cm.gimac.des.homologation.service.dto.CampaignResultDTO;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing {@link cm.gimac.des.homologation.domain.CampaignResult}.
 */
public interface CampaignResultService {
    /**
     * Save a campaignResult.
     *
     * @param campaignResultDTO the entity to save.
     * @return the persisted entity.
     */
    CampaignResultDTO save(CampaignResultDTO campaignResultDTO);

    /**
     * Updates a campaignResult.
     *
     * @param campaignResultDTO the entity to update.
     * @return the persisted entity.
     */
    CampaignResultDTO update(CampaignResultDTO campaignResultDTO);

    /**
     * Partially updates a campaignResult.
     *
     * @param campaignResultDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<CampaignResultDTO> partialUpdate(CampaignResultDTO campaignResultDTO);

    /**
     * Get all the campaignResults.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<CampaignResultDTO> findAll(Pageable pageable);

    /**
     * Get the "id" campaignResult.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<CampaignResultDTO> findOne(UUID id);

    /**
     * Delete the "id" campaignResult.
     *
     * @param id the id of the entity.
     */
    void delete(UUID id);
}
