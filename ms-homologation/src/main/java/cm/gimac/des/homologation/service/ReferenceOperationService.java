package cm.gimac.des.homologation.service;

import cm.gimac.des.homologation.service.dto.ReferenceOperationDTO;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service Interface for managing {@link cm.gimac.des.homologation.domain.ReferenceOperation}.
 */
public interface ReferenceOperationService {
    /**
     * Save a referenceOperation.
     *
     * @param referenceOperationDTO the entity to save.
     * @return the persisted entity.
     */
    ReferenceOperationDTO save(ReferenceOperationDTO referenceOperationDTO);

    /**
     * Updates a referenceOperation.
     *
     * @param referenceOperationDTO the entity to update.
     * @return the persisted entity.
     */
    ReferenceOperationDTO update(ReferenceOperationDTO referenceOperationDTO);

    /**
     * Partially updates a referenceOperation.
     *
     * @param referenceOperationDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<ReferenceOperationDTO> partialUpdate(ReferenceOperationDTO referenceOperationDTO);

    /**
     * Get all the referenceOperations.
     *
     * @return the list of entities.
     */
    List<ReferenceOperationDTO> findAll();

    /**
     * Get the "id" referenceOperation.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<ReferenceOperationDTO> findOne(UUID id);

    /**
     * Delete the "id" referenceOperation.
     *
     * @param id the id of the entity.
     */
    void delete(UUID id);
}
