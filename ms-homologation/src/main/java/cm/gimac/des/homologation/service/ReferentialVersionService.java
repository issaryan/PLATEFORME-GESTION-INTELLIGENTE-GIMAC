package cm.gimac.des.homologation.service;

import cm.gimac.des.homologation.service.dto.ReferentialVersionDTO;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service Interface for managing {@link cm.gimac.des.homologation.domain.ReferentialVersion}.
 */
public interface ReferentialVersionService {
    /**
     * Save a referentialVersion.
     *
     * @param referentialVersionDTO the entity to save.
     * @return the persisted entity.
     */
    ReferentialVersionDTO save(ReferentialVersionDTO referentialVersionDTO);

    /**
     * Updates a referentialVersion.
     *
     * @param referentialVersionDTO the entity to update.
     * @return the persisted entity.
     */
    ReferentialVersionDTO update(ReferentialVersionDTO referentialVersionDTO);

    /**
     * Partially updates a referentialVersion.
     *
     * @param referentialVersionDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<ReferentialVersionDTO> partialUpdate(ReferentialVersionDTO referentialVersionDTO);

    /**
     * Get all the referentialVersions.
     *
     * @return the list of entities.
     */
    List<ReferentialVersionDTO> findAll();

    /**
     * Get the "id" referentialVersion.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<ReferentialVersionDTO> findOne(UUID id);

    /**
     * Delete the "id" referentialVersion.
     *
     * @param id the id of the entity.
     */
    void delete(UUID id);
}
