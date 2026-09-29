package cm.gimac.des.homologation.service;

import cm.gimac.des.homologation.service.dto.ReferentialMigrationDTO;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service Interface for managing {@link cm.gimac.des.homologation.domain.ReferentialMigration}.
 */
public interface ReferentialMigrationService {
    /**
     * Save a referentialMigration.
     *
     * @param referentialMigrationDTO the entity to save.
     * @return the persisted entity.
     */
    ReferentialMigrationDTO save(ReferentialMigrationDTO referentialMigrationDTO);

    /**
     * Updates a referentialMigration.
     *
     * @param referentialMigrationDTO the entity to update.
     * @return the persisted entity.
     */
    ReferentialMigrationDTO update(ReferentialMigrationDTO referentialMigrationDTO);

    /**
     * Partially updates a referentialMigration.
     *
     * @param referentialMigrationDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<ReferentialMigrationDTO> partialUpdate(ReferentialMigrationDTO referentialMigrationDTO);

    /**
     * Get all the referentialMigrations.
     *
     * @return the list of entities.
     */
    List<ReferentialMigrationDTO> findAll();

    /**
     * Get the "id" referentialMigration.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<ReferentialMigrationDTO> findOne(UUID id);

    /**
     * Delete the "id" referentialMigration.
     *
     * @param id the id of the entity.
     */
    void delete(UUID id);
}
