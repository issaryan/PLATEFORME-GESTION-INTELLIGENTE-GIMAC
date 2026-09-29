package cm.gimac.des.certification.service;

import cm.gimac.des.certification.service.dto.AttestationStatusDecisionDTO;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service Interface for managing {@link cm.gimac.des.certification.domain.AttestationStatusDecision}.
 */
public interface AttestationStatusDecisionService {
    /**
     * Save a attestationStatusDecision.
     *
     * @param attestationStatusDecisionDTO the entity to save.
     * @return the persisted entity.
     */
    AttestationStatusDecisionDTO save(AttestationStatusDecisionDTO attestationStatusDecisionDTO);

    /**
     * Updates a attestationStatusDecision.
     *
     * @param attestationStatusDecisionDTO the entity to update.
     * @return the persisted entity.
     */
    AttestationStatusDecisionDTO update(AttestationStatusDecisionDTO attestationStatusDecisionDTO);

    /**
     * Partially updates a attestationStatusDecision.
     *
     * @param attestationStatusDecisionDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<AttestationStatusDecisionDTO> partialUpdate(AttestationStatusDecisionDTO attestationStatusDecisionDTO);

    /**
     * Get all the attestationStatusDecisions.
     *
     * @return the list of entities.
     */
    List<AttestationStatusDecisionDTO> findAll();

    /**
     * Get the "id" attestationStatusDecision.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<AttestationStatusDecisionDTO> findOne(UUID id);

    /**
     * Delete the "id" attestationStatusDecision.
     *
     * @param id the id of the entity.
     */
    void delete(UUID id);
}
