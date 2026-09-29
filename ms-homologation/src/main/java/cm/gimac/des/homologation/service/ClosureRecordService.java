package cm.gimac.des.homologation.service;

import cm.gimac.des.homologation.service.dto.ClosureRecordDTO;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service Interface for managing {@link cm.gimac.des.homologation.domain.ClosureRecord}.
 */
public interface ClosureRecordService {
    /**
     * Save a closureRecord.
     *
     * @param closureRecordDTO the entity to save.
     * @return the persisted entity.
     */
    ClosureRecordDTO save(ClosureRecordDTO closureRecordDTO);

    /**
     * Updates a closureRecord.
     *
     * @param closureRecordDTO the entity to update.
     * @return the persisted entity.
     */
    ClosureRecordDTO update(ClosureRecordDTO closureRecordDTO);

    /**
     * Partially updates a closureRecord.
     *
     * @param closureRecordDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<ClosureRecordDTO> partialUpdate(ClosureRecordDTO closureRecordDTO);

    /**
     * Get all the closureRecords.
     *
     * @return the list of entities.
     */
    List<ClosureRecordDTO> findAll();

    /**
     * Get the "id" closureRecord.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<ClosureRecordDTO> findOne(UUID id);

    /**
     * Delete the "id" closureRecord.
     *
     * @param id the id of the entity.
     */
    void delete(UUID id);
}
