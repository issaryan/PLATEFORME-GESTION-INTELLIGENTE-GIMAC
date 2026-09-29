package cm.gimac.des.homologation.service;

import cm.gimac.des.homologation.service.dto.LiaisonDTO;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing {@link cm.gimac.des.homologation.domain.Liaison}.
 */
public interface LiaisonService {
    /**
     * Save a liaison.
     *
     * @param liaisonDTO the entity to save.
     * @return the persisted entity.
     */
    LiaisonDTO save(LiaisonDTO liaisonDTO);

    /**
     * Updates a liaison.
     *
     * @param liaisonDTO the entity to update.
     * @return the persisted entity.
     */
    LiaisonDTO update(LiaisonDTO liaisonDTO);

    /**
     * Partially updates a liaison.
     *
     * @param liaisonDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<LiaisonDTO> partialUpdate(LiaisonDTO liaisonDTO);

    /**
     * Get all the liaisons.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<LiaisonDTO> findAll(Pageable pageable);

    /**
     * Get all the LiaisonDTO where ContractualScope is {@code null}.
     *
     * @return the {@link List} of entities.
     */
    List<LiaisonDTO> findAllWhereContractualScopeIsNull();

    /**
     * Get the "id" liaison.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<LiaisonDTO> findOne(UUID id);

    /**
     * Delete the "id" liaison.
     *
     * @param id the id of the entity.
     */
    void delete(UUID id);
}
