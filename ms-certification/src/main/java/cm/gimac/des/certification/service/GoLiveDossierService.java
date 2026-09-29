package cm.gimac.des.certification.service;

import cm.gimac.des.certification.service.dto.GoLiveDossierDTO;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service Interface for managing {@link cm.gimac.des.certification.domain.GoLiveDossier}.
 */
public interface GoLiveDossierService {
    /**
     * Save a goLiveDossier.
     *
     * @param goLiveDossierDTO the entity to save.
     * @return the persisted entity.
     */
    GoLiveDossierDTO save(GoLiveDossierDTO goLiveDossierDTO);

    /**
     * Updates a goLiveDossier.
     *
     * @param goLiveDossierDTO the entity to update.
     * @return the persisted entity.
     */
    GoLiveDossierDTO update(GoLiveDossierDTO goLiveDossierDTO);

    /**
     * Partially updates a goLiveDossier.
     *
     * @param goLiveDossierDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<GoLiveDossierDTO> partialUpdate(GoLiveDossierDTO goLiveDossierDTO);

    /**
     * Get all the goLiveDossiers.
     *
     * @return the list of entities.
     */
    List<GoLiveDossierDTO> findAll();

    /**
     * Get all the GoLiveDossierDTO where GoLiveReport is {@code null}.
     *
     * @return the {@link List} of entities.
     */
    List<GoLiveDossierDTO> findAllWhereGoLiveReportIsNull();
    /**
     * Get all the GoLiveDossierDTO where Attestation is {@code null}.
     *
     * @return the {@link List} of entities.
     */
    List<GoLiveDossierDTO> findAllWhereAttestationIsNull();

    /**
     * Get the "id" goLiveDossier.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<GoLiveDossierDTO> findOne(UUID id);

    /**
     * Delete the "id" goLiveDossier.
     *
     * @param id the id of the entity.
     */
    void delete(UUID id);
}
