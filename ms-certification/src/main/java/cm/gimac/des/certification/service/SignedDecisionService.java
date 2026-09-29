package cm.gimac.des.certification.service;

import cm.gimac.des.certification.service.dto.SignedDecisionDTO;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service Interface for managing {@link cm.gimac.des.certification.domain.SignedDecision}.
 */
public interface SignedDecisionService {
    /**
     * Save a signedDecision.
     *
     * @param signedDecisionDTO the entity to save.
     * @return the persisted entity.
     */
    SignedDecisionDTO save(SignedDecisionDTO signedDecisionDTO);

    /**
     * Updates a signedDecision.
     *
     * @param signedDecisionDTO the entity to update.
     * @return the persisted entity.
     */
    SignedDecisionDTO update(SignedDecisionDTO signedDecisionDTO);

    /**
     * Partially updates a signedDecision.
     *
     * @param signedDecisionDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<SignedDecisionDTO> partialUpdate(SignedDecisionDTO signedDecisionDTO);

    /**
     * Get all the signedDecisions.
     *
     * @return the list of entities.
     */
    List<SignedDecisionDTO> findAll();

    /**
     * Get the "id" signedDecision.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<SignedDecisionDTO> findOne(UUID id);

    /**
     * Delete the "id" signedDecision.
     *
     * @param id the id of the entity.
     */
    void delete(UUID id);
}
