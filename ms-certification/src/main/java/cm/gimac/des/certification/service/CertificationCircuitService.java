package cm.gimac.des.certification.service;

import cm.gimac.des.certification.service.dto.CertificationCircuitDTO;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service Interface for managing {@link cm.gimac.des.certification.domain.CertificationCircuit}.
 */
public interface CertificationCircuitService {
    /**
     * Save a certificationCircuit.
     *
     * @param certificationCircuitDTO the entity to save.
     * @return the persisted entity.
     */
    CertificationCircuitDTO save(CertificationCircuitDTO certificationCircuitDTO);

    /**
     * Updates a certificationCircuit.
     *
     * @param certificationCircuitDTO the entity to update.
     * @return the persisted entity.
     */
    CertificationCircuitDTO update(CertificationCircuitDTO certificationCircuitDTO);

    /**
     * Partially updates a certificationCircuit.
     *
     * @param certificationCircuitDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<CertificationCircuitDTO> partialUpdate(CertificationCircuitDTO certificationCircuitDTO);

    /**
     * Get all the certificationCircuits.
     *
     * @return the list of entities.
     */
    List<CertificationCircuitDTO> findAll();

    /**
     * Get all the CertificationCircuitDTO where GoLiveDossier is {@code null}.
     *
     * @return the {@link List} of entities.
     */
    List<CertificationCircuitDTO> findAllWhereGoLiveDossierIsNull();

    /**
     * Get the "id" certificationCircuit.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<CertificationCircuitDTO> findOne(UUID id);

    /**
     * Delete the "id" certificationCircuit.
     *
     * @param id the id of the entity.
     */
    void delete(UUID id);
}
