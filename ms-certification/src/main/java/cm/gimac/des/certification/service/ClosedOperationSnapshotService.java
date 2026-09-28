package cm.gimac.des.certification.service;

import cm.gimac.des.certification.domain.ClosedOperationSnapshot;
import cm.gimac.des.certification.repository.ClosedOperationSnapshotRepository;
import cm.gimac.des.certification.service.dto.ClosedOperationSnapshotDTO;
import cm.gimac.des.certification.service.mapper.ClosedOperationSnapshotMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.certification.domain.ClosedOperationSnapshot}.
 */
@Service
@Transactional
public class ClosedOperationSnapshotService {

    private static final Logger LOG = LoggerFactory.getLogger(ClosedOperationSnapshotService.class);

    private final ClosedOperationSnapshotRepository closedOperationSnapshotRepository;

    private final ClosedOperationSnapshotMapper closedOperationSnapshotMapper;

    public ClosedOperationSnapshotService(
        ClosedOperationSnapshotRepository closedOperationSnapshotRepository,
        ClosedOperationSnapshotMapper closedOperationSnapshotMapper
    ) {
        this.closedOperationSnapshotRepository = closedOperationSnapshotRepository;
        this.closedOperationSnapshotMapper = closedOperationSnapshotMapper;
    }

    /**
     * Save a closedOperationSnapshot.
     *
     * @param closedOperationSnapshotDTO the entity to save.
     * @return the persisted entity.
     */
    public ClosedOperationSnapshotDTO save(ClosedOperationSnapshotDTO closedOperationSnapshotDTO) {
        LOG.debug("Request to save ClosedOperationSnapshot : {}", closedOperationSnapshotDTO);
        ClosedOperationSnapshot closedOperationSnapshot = closedOperationSnapshotMapper.toEntity(closedOperationSnapshotDTO);
        closedOperationSnapshot = closedOperationSnapshotRepository.save(closedOperationSnapshot);
        return closedOperationSnapshotMapper.toDto(closedOperationSnapshot);
    }

    /**
     * Update a closedOperationSnapshot.
     *
     * @param closedOperationSnapshotDTO the entity to save.
     * @return the persisted entity.
     */
    public ClosedOperationSnapshotDTO update(ClosedOperationSnapshotDTO closedOperationSnapshotDTO) {
        LOG.debug("Request to update ClosedOperationSnapshot : {}", closedOperationSnapshotDTO);
        ClosedOperationSnapshot closedOperationSnapshot = closedOperationSnapshotMapper.toEntity(closedOperationSnapshotDTO);
        closedOperationSnapshot = closedOperationSnapshotRepository.save(closedOperationSnapshot);
        return closedOperationSnapshotMapper.toDto(closedOperationSnapshot);
    }

    /**
     * Partially update a closedOperationSnapshot.
     *
     * @param closedOperationSnapshotDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<ClosedOperationSnapshotDTO> partialUpdate(ClosedOperationSnapshotDTO closedOperationSnapshotDTO) {
        LOG.debug("Request to partially update ClosedOperationSnapshot : {}", closedOperationSnapshotDTO);

        return closedOperationSnapshotRepository
            .findById(closedOperationSnapshotDTO.getId())
            .map(existingClosedOperationSnapshot -> {
                closedOperationSnapshotMapper.partialUpdate(existingClosedOperationSnapshot, closedOperationSnapshotDTO);

                return existingClosedOperationSnapshot;
            })
            .map(closedOperationSnapshotRepository::save)
            .map(closedOperationSnapshotMapper::toDto);
    }

    /**
     * Get one closedOperationSnapshot by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<ClosedOperationSnapshotDTO> findOne(UUID id) {
        LOG.debug("Request to get ClosedOperationSnapshot : {}", id);
        return closedOperationSnapshotRepository.findById(id).map(closedOperationSnapshotMapper::toDto);
    }

    /**
     * Delete the closedOperationSnapshot by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete ClosedOperationSnapshot : {}", id);
        closedOperationSnapshotRepository.deleteById(id);
    }
}
