package cm.gimac.des.certification.service;

import cm.gimac.des.certification.service.dto.CompletenessConditionDTO;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service Interface for managing {@link cm.gimac.des.certification.domain.CompletenessCondition}.
 */
public interface CompletenessConditionService {
    /**
     * Save a completenessCondition.
     *
     * @param completenessConditionDTO the entity to save.
     * @return the persisted entity.
     */
    CompletenessConditionDTO save(CompletenessConditionDTO completenessConditionDTO);

    /**
     * Updates a completenessCondition.
     *
     * @param completenessConditionDTO the entity to update.
     * @return the persisted entity.
     */
    CompletenessConditionDTO update(CompletenessConditionDTO completenessConditionDTO);

    /**
     * Partially updates a completenessCondition.
     *
     * @param completenessConditionDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<CompletenessConditionDTO> partialUpdate(CompletenessConditionDTO completenessConditionDTO);

    /**
     * Get all the completenessConditions.
     *
     * @return the list of entities.
     */
    List<CompletenessConditionDTO> findAll();

    /**
     * Get the "id" completenessCondition.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<CompletenessConditionDTO> findOne(UUID id);

    /**
     * Delete the "id" completenessCondition.
     *
     * @param id the id of the entity.
     */
    void delete(UUID id);
}
