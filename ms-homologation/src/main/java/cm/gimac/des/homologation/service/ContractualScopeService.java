package cm.gimac.des.homologation.service;

import cm.gimac.des.homologation.service.dto.ContractualScopeDTO;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service Interface for managing {@link cm.gimac.des.homologation.domain.ContractualScope}.
 */
public interface ContractualScopeService {
    /**
     * Save a contractualScope.
     *
     * @param contractualScopeDTO the entity to save.
     * @return the persisted entity.
     */
    ContractualScopeDTO save(ContractualScopeDTO contractualScopeDTO);

    /**
     * Updates a contractualScope.
     *
     * @param contractualScopeDTO the entity to update.
     * @return the persisted entity.
     */
    ContractualScopeDTO update(ContractualScopeDTO contractualScopeDTO);

    /**
     * Partially updates a contractualScope.
     *
     * @param contractualScopeDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<ContractualScopeDTO> partialUpdate(ContractualScopeDTO contractualScopeDTO);

    /**
     * Get all the contractualScopes.
     *
     * @return the list of entities.
     */
    List<ContractualScopeDTO> findAll();

    /**
     * Get the "id" contractualScope.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<ContractualScopeDTO> findOne(UUID id);

    /**
     * Delete the "id" contractualScope.
     *
     * @param id the id of the entity.
     */
    void delete(UUID id);
}
