package cm.gimac.des.audit.service;

import cm.gimac.des.audit.domain.DeduplicationEntry;
import cm.gimac.des.audit.repository.DeduplicationEntryRepository;
import cm.gimac.des.audit.service.dto.DeduplicationEntryDTO;
import cm.gimac.des.audit.service.mapper.DeduplicationEntryMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.audit.domain.DeduplicationEntry}.
 */
@Service
@Transactional
public class DeduplicationEntryService {

    private static final Logger LOG = LoggerFactory.getLogger(DeduplicationEntryService.class);

    private final DeduplicationEntryRepository deduplicationEntryRepository;

    private final DeduplicationEntryMapper deduplicationEntryMapper;

    public DeduplicationEntryService(
        DeduplicationEntryRepository deduplicationEntryRepository,
        DeduplicationEntryMapper deduplicationEntryMapper
    ) {
        this.deduplicationEntryRepository = deduplicationEntryRepository;
        this.deduplicationEntryMapper = deduplicationEntryMapper;
    }

    /**
     * Save a deduplicationEntry.
     *
     * @param deduplicationEntryDTO the entity to save.
     * @return the persisted entity.
     */
    public DeduplicationEntryDTO save(DeduplicationEntryDTO deduplicationEntryDTO) {
        LOG.debug("Request to save DeduplicationEntry : {}", deduplicationEntryDTO);
        DeduplicationEntry deduplicationEntry = deduplicationEntryMapper.toEntity(deduplicationEntryDTO);
        deduplicationEntry = deduplicationEntryRepository.save(deduplicationEntry);
        return deduplicationEntryMapper.toDto(deduplicationEntry);
    }

    /**
     * Update a deduplicationEntry.
     *
     * @param deduplicationEntryDTO the entity to save.
     * @return the persisted entity.
     */
    public DeduplicationEntryDTO update(DeduplicationEntryDTO deduplicationEntryDTO) {
        LOG.debug("Request to update DeduplicationEntry : {}", deduplicationEntryDTO);
        DeduplicationEntry deduplicationEntry = deduplicationEntryMapper.toEntity(deduplicationEntryDTO);
        deduplicationEntry = deduplicationEntryRepository.save(deduplicationEntry);
        return deduplicationEntryMapper.toDto(deduplicationEntry);
    }

    /**
     * Partially update a deduplicationEntry.
     *
     * @param deduplicationEntryDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<DeduplicationEntryDTO> partialUpdate(DeduplicationEntryDTO deduplicationEntryDTO) {
        LOG.debug("Request to partially update DeduplicationEntry : {}", deduplicationEntryDTO);

        return deduplicationEntryRepository
            .findById(deduplicationEntryDTO.getId())
            .map(existingDeduplicationEntry -> {
                deduplicationEntryMapper.partialUpdate(existingDeduplicationEntry, deduplicationEntryDTO);

                return existingDeduplicationEntry;
            })
            .map(deduplicationEntryRepository::save)
            .map(deduplicationEntryMapper::toDto);
    }

    /**
     * Get all the deduplicationEntries.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<DeduplicationEntryDTO> findAll() {
        LOG.debug("Request to get all DeduplicationEntries");
        return deduplicationEntryRepository
            .findAll()
            .stream()
            .map(deduplicationEntryMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one deduplicationEntry by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<DeduplicationEntryDTO> findOne(UUID id) {
        LOG.debug("Request to get DeduplicationEntry : {}", id);
        return deduplicationEntryRepository.findById(id).map(deduplicationEntryMapper::toDto);
    }

    /**
     * Delete the deduplicationEntry by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete DeduplicationEntry : {}", id);
        deduplicationEntryRepository.deleteById(id);
    }
}
