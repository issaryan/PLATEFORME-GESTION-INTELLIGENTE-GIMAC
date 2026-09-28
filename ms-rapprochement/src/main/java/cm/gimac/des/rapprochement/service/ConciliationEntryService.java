package cm.gimac.des.rapprochement.service;

import cm.gimac.des.rapprochement.domain.ConciliationEntry;
import cm.gimac.des.rapprochement.repository.ConciliationEntryRepository;
import cm.gimac.des.rapprochement.service.dto.ConciliationEntryDTO;
import cm.gimac.des.rapprochement.service.mapper.ConciliationEntryMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.rapprochement.domain.ConciliationEntry}.
 */
@Service
@Transactional
public class ConciliationEntryService {

    private static final Logger LOG = LoggerFactory.getLogger(ConciliationEntryService.class);

    private final ConciliationEntryRepository conciliationEntryRepository;

    private final ConciliationEntryMapper conciliationEntryMapper;

    public ConciliationEntryService(
        ConciliationEntryRepository conciliationEntryRepository,
        ConciliationEntryMapper conciliationEntryMapper
    ) {
        this.conciliationEntryRepository = conciliationEntryRepository;
        this.conciliationEntryMapper = conciliationEntryMapper;
    }

    /**
     * Save a conciliationEntry.
     *
     * @param conciliationEntryDTO the entity to save.
     * @return the persisted entity.
     */
    public ConciliationEntryDTO save(ConciliationEntryDTO conciliationEntryDTO) {
        LOG.debug("Request to save ConciliationEntry : {}", conciliationEntryDTO);
        ConciliationEntry conciliationEntry = conciliationEntryMapper.toEntity(conciliationEntryDTO);
        conciliationEntry = conciliationEntryRepository.save(conciliationEntry);
        return conciliationEntryMapper.toDto(conciliationEntry);
    }

    /**
     * Update a conciliationEntry.
     *
     * @param conciliationEntryDTO the entity to save.
     * @return the persisted entity.
     */
    public ConciliationEntryDTO update(ConciliationEntryDTO conciliationEntryDTO) {
        LOG.debug("Request to update ConciliationEntry : {}", conciliationEntryDTO);
        ConciliationEntry conciliationEntry = conciliationEntryMapper.toEntity(conciliationEntryDTO);
        conciliationEntry = conciliationEntryRepository.save(conciliationEntry);
        return conciliationEntryMapper.toDto(conciliationEntry);
    }

    /**
     * Partially update a conciliationEntry.
     *
     * @param conciliationEntryDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<ConciliationEntryDTO> partialUpdate(ConciliationEntryDTO conciliationEntryDTO) {
        LOG.debug("Request to partially update ConciliationEntry : {}", conciliationEntryDTO);

        return conciliationEntryRepository
            .findById(conciliationEntryDTO.getId())
            .map(existingConciliationEntry -> {
                conciliationEntryMapper.partialUpdate(existingConciliationEntry, conciliationEntryDTO);

                return existingConciliationEntry;
            })
            .map(conciliationEntryRepository::save)
            .map(conciliationEntryMapper::toDto);
    }

    /**
     * Get all the conciliationEntries.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<ConciliationEntryDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all ConciliationEntries");
        return conciliationEntryRepository.findAll(pageable).map(conciliationEntryMapper::toDto);
    }

    /**
     * Get all the conciliationEntries with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<ConciliationEntryDTO> findAllWithEagerRelationships(Pageable pageable) {
        return conciliationEntryRepository.findAllWithEagerRelationships(pageable).map(conciliationEntryMapper::toDto);
    }

    /**
     * Get one conciliationEntry by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<ConciliationEntryDTO> findOne(UUID id) {
        LOG.debug("Request to get ConciliationEntry : {}", id);
        return conciliationEntryRepository.findOneWithEagerRelationships(id).map(conciliationEntryMapper::toDto);
    }

    /**
     * Delete the conciliationEntry by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete ConciliationEntry : {}", id);
        conciliationEntryRepository.deleteById(id);
    }
}
