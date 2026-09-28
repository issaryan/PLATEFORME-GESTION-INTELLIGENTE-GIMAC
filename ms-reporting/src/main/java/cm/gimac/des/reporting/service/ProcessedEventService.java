package cm.gimac.des.reporting.service;

import cm.gimac.des.reporting.domain.ProcessedEvent;
import cm.gimac.des.reporting.repository.ProcessedEventRepository;
import cm.gimac.des.reporting.service.dto.ProcessedEventDTO;
import cm.gimac.des.reporting.service.mapper.ProcessedEventMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.reporting.domain.ProcessedEvent}.
 */
@Service
@Transactional
public class ProcessedEventService {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessedEventService.class);

    private final ProcessedEventRepository processedEventRepository;

    private final ProcessedEventMapper processedEventMapper;

    public ProcessedEventService(ProcessedEventRepository processedEventRepository, ProcessedEventMapper processedEventMapper) {
        this.processedEventRepository = processedEventRepository;
        this.processedEventMapper = processedEventMapper;
    }

    /**
     * Save a processedEvent.
     *
     * @param processedEventDTO the entity to save.
     * @return the persisted entity.
     */
    public ProcessedEventDTO save(ProcessedEventDTO processedEventDTO) {
        LOG.debug("Request to save ProcessedEvent : {}", processedEventDTO);
        ProcessedEvent processedEvent = processedEventMapper.toEntity(processedEventDTO);
        processedEvent = processedEventRepository.save(processedEvent);
        return processedEventMapper.toDto(processedEvent);
    }

    /**
     * Update a processedEvent.
     *
     * @param processedEventDTO the entity to save.
     * @return the persisted entity.
     */
    public ProcessedEventDTO update(ProcessedEventDTO processedEventDTO) {
        LOG.debug("Request to update ProcessedEvent : {}", processedEventDTO);
        ProcessedEvent processedEvent = processedEventMapper.toEntity(processedEventDTO);
        processedEvent = processedEventRepository.save(processedEvent);
        return processedEventMapper.toDto(processedEvent);
    }

    /**
     * Partially update a processedEvent.
     *
     * @param processedEventDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<ProcessedEventDTO> partialUpdate(ProcessedEventDTO processedEventDTO) {
        LOG.debug("Request to partially update ProcessedEvent : {}", processedEventDTO);

        return processedEventRepository
            .findById(processedEventDTO.getId())
            .map(existingProcessedEvent -> {
                processedEventMapper.partialUpdate(existingProcessedEvent, processedEventDTO);

                return existingProcessedEvent;
            })
            .map(processedEventRepository::save)
            .map(processedEventMapper::toDto);
    }

    /**
     * Get all the processedEvents.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<ProcessedEventDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all ProcessedEvents");
        return processedEventRepository.findAll(pageable).map(processedEventMapper::toDto);
    }

    /**
     * Get one processedEvent by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<ProcessedEventDTO> findOne(UUID id) {
        LOG.debug("Request to get ProcessedEvent : {}", id);
        return processedEventRepository.findById(id).map(processedEventMapper::toDto);
    }

    /**
     * Delete the processedEvent by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete ProcessedEvent : {}", id);
        processedEventRepository.deleteById(id);
    }
}
