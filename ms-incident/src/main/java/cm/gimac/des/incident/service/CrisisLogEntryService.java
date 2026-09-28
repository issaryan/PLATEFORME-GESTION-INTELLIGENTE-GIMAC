package cm.gimac.des.incident.service;

import cm.gimac.des.incident.domain.CrisisLogEntry;
import cm.gimac.des.incident.repository.CrisisLogEntryRepository;
import cm.gimac.des.incident.service.dto.CrisisLogEntryDTO;
import cm.gimac.des.incident.service.mapper.CrisisLogEntryMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.incident.domain.CrisisLogEntry}.
 */
@Service
@Transactional
public class CrisisLogEntryService {

    private static final Logger LOG = LoggerFactory.getLogger(CrisisLogEntryService.class);

    private final CrisisLogEntryRepository crisisLogEntryRepository;

    private final CrisisLogEntryMapper crisisLogEntryMapper;

    public CrisisLogEntryService(CrisisLogEntryRepository crisisLogEntryRepository, CrisisLogEntryMapper crisisLogEntryMapper) {
        this.crisisLogEntryRepository = crisisLogEntryRepository;
        this.crisisLogEntryMapper = crisisLogEntryMapper;
    }

    /**
     * Save a crisisLogEntry.
     *
     * @param crisisLogEntryDTO the entity to save.
     * @return the persisted entity.
     */
    public CrisisLogEntryDTO save(CrisisLogEntryDTO crisisLogEntryDTO) {
        LOG.debug("Request to save CrisisLogEntry : {}", crisisLogEntryDTO);
        CrisisLogEntry crisisLogEntry = crisisLogEntryMapper.toEntity(crisisLogEntryDTO);
        crisisLogEntry = crisisLogEntryRepository.save(crisisLogEntry);
        return crisisLogEntryMapper.toDto(crisisLogEntry);
    }

    /**
     * Update a crisisLogEntry.
     *
     * @param crisisLogEntryDTO the entity to save.
     * @return the persisted entity.
     */
    public CrisisLogEntryDTO update(CrisisLogEntryDTO crisisLogEntryDTO) {
        LOG.debug("Request to update CrisisLogEntry : {}", crisisLogEntryDTO);
        CrisisLogEntry crisisLogEntry = crisisLogEntryMapper.toEntity(crisisLogEntryDTO);
        crisisLogEntry = crisisLogEntryRepository.save(crisisLogEntry);
        return crisisLogEntryMapper.toDto(crisisLogEntry);
    }

    /**
     * Partially update a crisisLogEntry.
     *
     * @param crisisLogEntryDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<CrisisLogEntryDTO> partialUpdate(CrisisLogEntryDTO crisisLogEntryDTO) {
        LOG.debug("Request to partially update CrisisLogEntry : {}", crisisLogEntryDTO);

        return crisisLogEntryRepository
            .findById(crisisLogEntryDTO.getId())
            .map(existingCrisisLogEntry -> {
                crisisLogEntryMapper.partialUpdate(existingCrisisLogEntry, crisisLogEntryDTO);

                return existingCrisisLogEntry;
            })
            .map(crisisLogEntryRepository::save)
            .map(crisisLogEntryMapper::toDto);
    }

    /**
     * Get all the crisisLogEntries with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<CrisisLogEntryDTO> findAllWithEagerRelationships(Pageable pageable) {
        return crisisLogEntryRepository.findAllWithEagerRelationships(pageable).map(crisisLogEntryMapper::toDto);
    }

    /**
     * Get one crisisLogEntry by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<CrisisLogEntryDTO> findOne(UUID id) {
        LOG.debug("Request to get CrisisLogEntry : {}", id);
        return crisisLogEntryRepository.findOneWithEagerRelationships(id).map(crisisLogEntryMapper::toDto);
    }

    /**
     * Delete the crisisLogEntry by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete CrisisLogEntry : {}", id);
        crisisLogEntryRepository.deleteById(id);
    }
}
