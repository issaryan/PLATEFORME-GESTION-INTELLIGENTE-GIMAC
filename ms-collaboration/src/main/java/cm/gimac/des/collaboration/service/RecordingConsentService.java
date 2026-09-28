package cm.gimac.des.collaboration.service;

import cm.gimac.des.collaboration.domain.RecordingConsent;
import cm.gimac.des.collaboration.repository.RecordingConsentRepository;
import cm.gimac.des.collaboration.service.dto.RecordingConsentDTO;
import cm.gimac.des.collaboration.service.mapper.RecordingConsentMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.collaboration.domain.RecordingConsent}.
 */
@Service
@Transactional
public class RecordingConsentService {

    private static final Logger LOG = LoggerFactory.getLogger(RecordingConsentService.class);

    private final RecordingConsentRepository recordingConsentRepository;

    private final RecordingConsentMapper recordingConsentMapper;

    public RecordingConsentService(RecordingConsentRepository recordingConsentRepository, RecordingConsentMapper recordingConsentMapper) {
        this.recordingConsentRepository = recordingConsentRepository;
        this.recordingConsentMapper = recordingConsentMapper;
    }

    /**
     * Save a recordingConsent.
     *
     * @param recordingConsentDTO the entity to save.
     * @return the persisted entity.
     */
    public RecordingConsentDTO save(RecordingConsentDTO recordingConsentDTO) {
        LOG.debug("Request to save RecordingConsent : {}", recordingConsentDTO);
        RecordingConsent recordingConsent = recordingConsentMapper.toEntity(recordingConsentDTO);
        recordingConsent = recordingConsentRepository.save(recordingConsent);
        return recordingConsentMapper.toDto(recordingConsent);
    }

    /**
     * Update a recordingConsent.
     *
     * @param recordingConsentDTO the entity to save.
     * @return the persisted entity.
     */
    public RecordingConsentDTO update(RecordingConsentDTO recordingConsentDTO) {
        LOG.debug("Request to update RecordingConsent : {}", recordingConsentDTO);
        RecordingConsent recordingConsent = recordingConsentMapper.toEntity(recordingConsentDTO);
        recordingConsent = recordingConsentRepository.save(recordingConsent);
        return recordingConsentMapper.toDto(recordingConsent);
    }

    /**
     * Partially update a recordingConsent.
     *
     * @param recordingConsentDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<RecordingConsentDTO> partialUpdate(RecordingConsentDTO recordingConsentDTO) {
        LOG.debug("Request to partially update RecordingConsent : {}", recordingConsentDTO);

        return recordingConsentRepository
            .findById(recordingConsentDTO.getId())
            .map(existingRecordingConsent -> {
                recordingConsentMapper.partialUpdate(existingRecordingConsent, recordingConsentDTO);

                return existingRecordingConsent;
            })
            .map(recordingConsentRepository::save)
            .map(recordingConsentMapper::toDto);
    }

    /**
     * Get all the recordingConsents.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<RecordingConsentDTO> findAll() {
        LOG.debug("Request to get all RecordingConsents");
        return recordingConsentRepository
            .findAll()
            .stream()
            .map(recordingConsentMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all the recordingConsents with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<RecordingConsentDTO> findAllWithEagerRelationships(Pageable pageable) {
        return recordingConsentRepository.findAllWithEagerRelationships(pageable).map(recordingConsentMapper::toDto);
    }

    /**
     * Get one recordingConsent by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<RecordingConsentDTO> findOne(UUID id) {
        LOG.debug("Request to get RecordingConsent : {}", id);
        return recordingConsentRepository.findOneWithEagerRelationships(id).map(recordingConsentMapper::toDto);
    }

    /**
     * Delete the recordingConsent by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete RecordingConsent : {}", id);
        recordingConsentRepository.deleteById(id);
    }
}
