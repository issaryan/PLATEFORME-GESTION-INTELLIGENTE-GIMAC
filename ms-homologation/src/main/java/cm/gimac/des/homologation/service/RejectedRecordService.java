package cm.gimac.des.homologation.service;

import cm.gimac.des.homologation.service.dto.RejectedRecordDTO;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service Interface for managing {@link cm.gimac.des.homologation.domain.RejectedRecord}.
 */
public interface RejectedRecordService {
    /**
     * Save a rejectedRecord.
     *
     * @param rejectedRecordDTO the entity to save.
     * @return the persisted entity.
     */
    RejectedRecordDTO save(RejectedRecordDTO rejectedRecordDTO);

    /**
     * Updates a rejectedRecord.
     *
     * @param rejectedRecordDTO the entity to update.
     * @return the persisted entity.
     */
    RejectedRecordDTO update(RejectedRecordDTO rejectedRecordDTO);

    /**
     * Partially updates a rejectedRecord.
     *
     * @param rejectedRecordDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<RejectedRecordDTO> partialUpdate(RejectedRecordDTO rejectedRecordDTO);

    /**
     * Get all the rejectedRecords.
     *
     * @return the list of entities.
     */
    List<RejectedRecordDTO> findAll();

    /**
     * Get the "id" rejectedRecord.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<RejectedRecordDTO> findOne(UUID id);

    /**
     * Delete the "id" rejectedRecord.
     *
     * @param id the id of the entity.
     */
    void delete(UUID id);
}
