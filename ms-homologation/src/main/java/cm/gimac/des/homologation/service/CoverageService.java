package cm.gimac.des.homologation.service;

import cm.gimac.des.homologation.service.dto.CoverageDTO;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service Interface for managing {@link cm.gimac.des.homologation.domain.Coverage}.
 */
public interface CoverageService {
    /**
     * Save a coverage.
     *
     * @param coverageDTO the entity to save.
     * @return the persisted entity.
     */
    CoverageDTO save(CoverageDTO coverageDTO);

    /**
     * Updates a coverage.
     *
     * @param coverageDTO the entity to update.
     * @return the persisted entity.
     */
    CoverageDTO update(CoverageDTO coverageDTO);

    /**
     * Partially updates a coverage.
     *
     * @param coverageDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<CoverageDTO> partialUpdate(CoverageDTO coverageDTO);

    /**
     * Get all the coverages.
     *
     * @return the list of entities.
     */
    List<CoverageDTO> findAll();

    /**
     * Get the "id" coverage.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<CoverageDTO> findOne(UUID id);

    /**
     * Delete the "id" coverage.
     *
     * @param id the id of the entity.
     */
    void delete(UUID id);
}
