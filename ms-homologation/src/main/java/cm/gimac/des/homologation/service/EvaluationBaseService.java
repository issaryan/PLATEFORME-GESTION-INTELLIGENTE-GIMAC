package cm.gimac.des.homologation.service;

import cm.gimac.des.homologation.service.dto.EvaluationBaseDTO;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service Interface for managing {@link cm.gimac.des.homologation.domain.EvaluationBase}.
 */
public interface EvaluationBaseService {
    /**
     * Save a evaluationBase.
     *
     * @param evaluationBaseDTO the entity to save.
     * @return the persisted entity.
     */
    EvaluationBaseDTO save(EvaluationBaseDTO evaluationBaseDTO);

    /**
     * Updates a evaluationBase.
     *
     * @param evaluationBaseDTO the entity to update.
     * @return the persisted entity.
     */
    EvaluationBaseDTO update(EvaluationBaseDTO evaluationBaseDTO);

    /**
     * Partially updates a evaluationBase.
     *
     * @param evaluationBaseDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<EvaluationBaseDTO> partialUpdate(EvaluationBaseDTO evaluationBaseDTO);

    /**
     * Get all the evaluationBases.
     *
     * @return the list of entities.
     */
    List<EvaluationBaseDTO> findAll();

    /**
     * Get the "id" evaluationBase.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<EvaluationBaseDTO> findOne(UUID id);

    /**
     * Delete the "id" evaluationBase.
     *
     * @param id the id of the entity.
     */
    void delete(UUID id);
}
