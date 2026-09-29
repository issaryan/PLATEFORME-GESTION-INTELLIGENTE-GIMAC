package cm.gimac.des.homologation.service;

import cm.gimac.des.homologation.service.dto.EvidenceReferenceDTO;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service Interface for managing {@link cm.gimac.des.homologation.domain.EvidenceReference}.
 */
public interface EvidenceReferenceService {
    /**
     * Save a evidenceReference.
     *
     * @param evidenceReferenceDTO the entity to save.
     * @return the persisted entity.
     */
    EvidenceReferenceDTO save(EvidenceReferenceDTO evidenceReferenceDTO);

    /**
     * Updates a evidenceReference.
     *
     * @param evidenceReferenceDTO the entity to update.
     * @return the persisted entity.
     */
    EvidenceReferenceDTO update(EvidenceReferenceDTO evidenceReferenceDTO);

    /**
     * Partially updates a evidenceReference.
     *
     * @param evidenceReferenceDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<EvidenceReferenceDTO> partialUpdate(EvidenceReferenceDTO evidenceReferenceDTO);

    /**
     * Get all the evidenceReferences.
     *
     * @return the list of entities.
     */
    List<EvidenceReferenceDTO> findAll();

    /**
     * Get the "id" evidenceReference.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<EvidenceReferenceDTO> findOne(UUID id);

    /**
     * Delete the "id" evidenceReference.
     *
     * @param id the id of the entity.
     */
    void delete(UUID id);
}
