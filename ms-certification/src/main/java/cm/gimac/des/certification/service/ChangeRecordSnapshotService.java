package cm.gimac.des.certification.service;

import cm.gimac.des.certification.service.dto.ChangeRecordSnapshotDTO;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service Interface for managing {@link cm.gimac.des.certification.domain.ChangeRecordSnapshot}.
 */
public interface ChangeRecordSnapshotService {
    /**
     * Save a changeRecordSnapshot.
     *
     * @param changeRecordSnapshotDTO the entity to save.
     * @return the persisted entity.
     */
    ChangeRecordSnapshotDTO save(ChangeRecordSnapshotDTO changeRecordSnapshotDTO);

    /**
     * Updates a changeRecordSnapshot.
     *
     * @param changeRecordSnapshotDTO the entity to update.
     * @return the persisted entity.
     */
    ChangeRecordSnapshotDTO update(ChangeRecordSnapshotDTO changeRecordSnapshotDTO);

    /**
     * Partially updates a changeRecordSnapshot.
     *
     * @param changeRecordSnapshotDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<ChangeRecordSnapshotDTO> partialUpdate(ChangeRecordSnapshotDTO changeRecordSnapshotDTO);

    /**
     * Get all the changeRecordSnapshots.
     *
     * @return the list of entities.
     */
    List<ChangeRecordSnapshotDTO> findAll();

    /**
     * Get the "id" changeRecordSnapshot.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<ChangeRecordSnapshotDTO> findOne(UUID id);

    /**
     * Delete the "id" changeRecordSnapshot.
     *
     * @param id the id of the entity.
     */
    void delete(UUID id);
}
