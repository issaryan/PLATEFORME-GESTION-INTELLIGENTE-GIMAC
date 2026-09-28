package cm.gimac.des.monitoring.service;

import cm.gimac.des.monitoring.domain.ShiftLogEntry;
import cm.gimac.des.monitoring.repository.ShiftLogEntryRepository;
import cm.gimac.des.monitoring.service.dto.ShiftLogEntryDTO;
import cm.gimac.des.monitoring.service.mapper.ShiftLogEntryMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.monitoring.domain.ShiftLogEntry}.
 */
@Service
@Transactional
public class ShiftLogEntryService {

    private static final Logger LOG = LoggerFactory.getLogger(ShiftLogEntryService.class);

    private final ShiftLogEntryRepository shiftLogEntryRepository;

    private final ShiftLogEntryMapper shiftLogEntryMapper;

    public ShiftLogEntryService(ShiftLogEntryRepository shiftLogEntryRepository, ShiftLogEntryMapper shiftLogEntryMapper) {
        this.shiftLogEntryRepository = shiftLogEntryRepository;
        this.shiftLogEntryMapper = shiftLogEntryMapper;
    }

    /**
     * Save a shiftLogEntry.
     *
     * @param shiftLogEntryDTO the entity to save.
     * @return the persisted entity.
     */
    public ShiftLogEntryDTO save(ShiftLogEntryDTO shiftLogEntryDTO) {
        LOG.debug("Request to save ShiftLogEntry : {}", shiftLogEntryDTO);
        ShiftLogEntry shiftLogEntry = shiftLogEntryMapper.toEntity(shiftLogEntryDTO);
        shiftLogEntry = shiftLogEntryRepository.save(shiftLogEntry);
        return shiftLogEntryMapper.toDto(shiftLogEntry);
    }

    /**
     * Update a shiftLogEntry.
     *
     * @param shiftLogEntryDTO the entity to save.
     * @return the persisted entity.
     */
    public ShiftLogEntryDTO update(ShiftLogEntryDTO shiftLogEntryDTO) {
        LOG.debug("Request to update ShiftLogEntry : {}", shiftLogEntryDTO);
        ShiftLogEntry shiftLogEntry = shiftLogEntryMapper.toEntity(shiftLogEntryDTO);
        shiftLogEntry = shiftLogEntryRepository.save(shiftLogEntry);
        return shiftLogEntryMapper.toDto(shiftLogEntry);
    }

    /**
     * Partially update a shiftLogEntry.
     *
     * @param shiftLogEntryDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<ShiftLogEntryDTO> partialUpdate(ShiftLogEntryDTO shiftLogEntryDTO) {
        LOG.debug("Request to partially update ShiftLogEntry : {}", shiftLogEntryDTO);

        return shiftLogEntryRepository
            .findById(shiftLogEntryDTO.getId())
            .map(existingShiftLogEntry -> {
                shiftLogEntryMapper.partialUpdate(existingShiftLogEntry, shiftLogEntryDTO);

                return existingShiftLogEntry;
            })
            .map(shiftLogEntryRepository::save)
            .map(shiftLogEntryMapper::toDto);
    }

    /**
     * Get all the shiftLogEntries with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<ShiftLogEntryDTO> findAllWithEagerRelationships(Pageable pageable) {
        return shiftLogEntryRepository.findAllWithEagerRelationships(pageable).map(shiftLogEntryMapper::toDto);
    }

    /**
     * Get one shiftLogEntry by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<ShiftLogEntryDTO> findOne(UUID id) {
        LOG.debug("Request to get ShiftLogEntry : {}", id);
        return shiftLogEntryRepository.findOneWithEagerRelationships(id).map(shiftLogEntryMapper::toDto);
    }

    /**
     * Delete the shiftLogEntry by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete ShiftLogEntry : {}", id);
        shiftLogEntryRepository.deleteById(id);
    }
}
