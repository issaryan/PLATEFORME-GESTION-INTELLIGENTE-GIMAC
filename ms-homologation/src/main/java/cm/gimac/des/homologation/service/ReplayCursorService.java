package cm.gimac.des.homologation.service;

import cm.gimac.des.homologation.service.dto.ReplayCursorDTO;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service Interface for managing {@link cm.gimac.des.homologation.domain.ReplayCursor}.
 */
public interface ReplayCursorService {
    /**
     * Save a replayCursor.
     *
     * @param replayCursorDTO the entity to save.
     * @return the persisted entity.
     */
    ReplayCursorDTO save(ReplayCursorDTO replayCursorDTO);

    /**
     * Updates a replayCursor.
     *
     * @param replayCursorDTO the entity to update.
     * @return the persisted entity.
     */
    ReplayCursorDTO update(ReplayCursorDTO replayCursorDTO);

    /**
     * Partially updates a replayCursor.
     *
     * @param replayCursorDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<ReplayCursorDTO> partialUpdate(ReplayCursorDTO replayCursorDTO);

    /**
     * Get all the replayCursors.
     *
     * @return the list of entities.
     */
    List<ReplayCursorDTO> findAll();

    /**
     * Get the "id" replayCursor.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<ReplayCursorDTO> findOne(UUID id);

    /**
     * Delete the "id" replayCursor.
     *
     * @param id the id of the entity.
     */
    void delete(UUID id);
}
