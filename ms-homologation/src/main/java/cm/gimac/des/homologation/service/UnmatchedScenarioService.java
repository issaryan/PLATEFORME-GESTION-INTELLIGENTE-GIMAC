package cm.gimac.des.homologation.service;

import cm.gimac.des.homologation.service.dto.UnmatchedScenarioDTO;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing {@link cm.gimac.des.homologation.domain.UnmatchedScenario}.
 */
public interface UnmatchedScenarioService {
    /**
     * Save a unmatchedScenario.
     *
     * @param unmatchedScenarioDTO the entity to save.
     * @return the persisted entity.
     */
    UnmatchedScenarioDTO save(UnmatchedScenarioDTO unmatchedScenarioDTO);

    /**
     * Updates a unmatchedScenario.
     *
     * @param unmatchedScenarioDTO the entity to update.
     * @return the persisted entity.
     */
    UnmatchedScenarioDTO update(UnmatchedScenarioDTO unmatchedScenarioDTO);

    /**
     * Partially updates a unmatchedScenario.
     *
     * @param unmatchedScenarioDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<UnmatchedScenarioDTO> partialUpdate(UnmatchedScenarioDTO unmatchedScenarioDTO);

    /**
     * Get all the unmatchedScenarios.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<UnmatchedScenarioDTO> findAll(Pageable pageable);

    /**
     * Get the "id" unmatchedScenario.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<UnmatchedScenarioDTO> findOne(UUID id);

    /**
     * Delete the "id" unmatchedScenario.
     *
     * @param id the id of the entity.
     */
    void delete(UUID id);
}
