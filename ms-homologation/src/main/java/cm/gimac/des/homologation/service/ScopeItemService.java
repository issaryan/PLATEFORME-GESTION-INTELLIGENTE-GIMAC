package cm.gimac.des.homologation.service;

import cm.gimac.des.homologation.service.dto.ScopeItemDTO;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service Interface for managing {@link cm.gimac.des.homologation.domain.ScopeItem}.
 */
public interface ScopeItemService {
    /**
     * Save a scopeItem.
     *
     * @param scopeItemDTO the entity to save.
     * @return the persisted entity.
     */
    ScopeItemDTO save(ScopeItemDTO scopeItemDTO);

    /**
     * Updates a scopeItem.
     *
     * @param scopeItemDTO the entity to update.
     * @return the persisted entity.
     */
    ScopeItemDTO update(ScopeItemDTO scopeItemDTO);

    /**
     * Partially updates a scopeItem.
     *
     * @param scopeItemDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<ScopeItemDTO> partialUpdate(ScopeItemDTO scopeItemDTO);

    /**
     * Get all the scopeItems.
     *
     * @return the list of entities.
     */
    List<ScopeItemDTO> findAll();

    /**
     * Get the "id" scopeItem.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<ScopeItemDTO> findOne(UUID id);

    /**
     * Delete the "id" scopeItem.
     *
     * @param id the id of the entity.
     */
    void delete(UUID id);
}
