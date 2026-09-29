package cm.gimac.des.certification.service;

import cm.gimac.des.certification.service.dto.AttestationDTO;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing {@link cm.gimac.des.certification.domain.Attestation}.
 */
public interface AttestationService {
    /**
     * Save a attestation.
     *
     * @param attestationDTO the entity to save.
     * @return the persisted entity.
     */
    AttestationDTO save(AttestationDTO attestationDTO);

    /**
     * Updates a attestation.
     *
     * @param attestationDTO the entity to update.
     * @return the persisted entity.
     */
    AttestationDTO update(AttestationDTO attestationDTO);

    /**
     * Partially updates a attestation.
     *
     * @param attestationDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<AttestationDTO> partialUpdate(AttestationDTO attestationDTO);

    /**
     * Get all the attestations.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<AttestationDTO> findAll(Pageable pageable);

    /**
     * Get the "id" attestation.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<AttestationDTO> findOne(UUID id);

    /**
     * Delete the "id" attestation.
     *
     * @param id the id of the entity.
     */
    void delete(UUID id);
}
