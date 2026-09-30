package cm.gimac.des.collaboration.service;

import cm.gimac.des.collaboration.domain.RecordingSegment;
import cm.gimac.des.collaboration.repository.RecordingSegmentRepository;
import cm.gimac.des.collaboration.service.dto.RecordingSegmentDTO;
import cm.gimac.des.collaboration.service.mapper.RecordingSegmentMapper;
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
 * Service Implementation for managing {@link cm.gimac.des.collaboration.domain.RecordingSegment}.
 */
@Service
@Transactional
public class RecordingSegmentService {

    private static final Logger LOG = LoggerFactory.getLogger(RecordingSegmentService.class);

    private final RecordingSegmentRepository recordingSegmentRepository;

    private final RecordingSegmentMapper recordingSegmentMapper;

    public RecordingSegmentService(RecordingSegmentRepository recordingSegmentRepository, RecordingSegmentMapper recordingSegmentMapper) {
        this.recordingSegmentRepository = recordingSegmentRepository;
        this.recordingSegmentMapper = recordingSegmentMapper;
    }

    /**
     * Save a recordingSegment.
     *
     * @param recordingSegmentDTO the entity to save.
     * @return the persisted entity.
     */
    public RecordingSegmentDTO save(RecordingSegmentDTO recordingSegmentDTO) {
        LOG.debug("Request to save RecordingSegment : {}", recordingSegmentDTO);
        RecordingSegment recordingSegment = recordingSegmentMapper.toEntity(recordingSegmentDTO);
        recordingSegment = recordingSegmentRepository.save(recordingSegment);
        return recordingSegmentMapper.toDto(recordingSegment);
    }

    /**
     * Update a recordingSegment.
     *
     * @param recordingSegmentDTO the entity to save.
     * @return the persisted entity.
     */
    public RecordingSegmentDTO update(RecordingSegmentDTO recordingSegmentDTO) {
        LOG.debug("Request to update RecordingSegment : {}", recordingSegmentDTO);
        RecordingSegment recordingSegment = recordingSegmentMapper.toEntity(recordingSegmentDTO);
        recordingSegment = recordingSegmentRepository.save(recordingSegment);
        return recordingSegmentMapper.toDto(recordingSegment);
    }

    /**
     * Partially update a recordingSegment.
     *
     * @param recordingSegmentDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<RecordingSegmentDTO> partialUpdate(RecordingSegmentDTO recordingSegmentDTO) {
        LOG.debug("Request to partially update RecordingSegment : {}", recordingSegmentDTO);

        return recordingSegmentRepository
            .findById(recordingSegmentDTO.getId())
            .map(existingRecordingSegment -> {
                recordingSegmentMapper.partialUpdate(existingRecordingSegment, recordingSegmentDTO);

                return existingRecordingSegment;
            })
            .map(recordingSegmentRepository::save)
            .map(recordingSegmentMapper::toDto);
    }

    /**
     * Get all the recordingSegments.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<RecordingSegmentDTO> findAll() {
        LOG.debug("Request to get all RecordingSegments");
        return recordingSegmentRepository
            .findAll()
            .stream()
            .map(recordingSegmentMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all the recordingSegments with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public List<RecordingSegmentDTO> findAllWithEagerRelationships() {
        return recordingSegmentRepository
            .findAllWithEagerRelationships()
            .stream()
            .map(recordingSegmentMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one recordingSegment by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<RecordingSegmentDTO> findOne(UUID id) {
        LOG.debug("Request to get RecordingSegment : {}", id);
        return recordingSegmentRepository.findOneWithEagerRelationships(id).map(recordingSegmentMapper::toDto);
    }

    /**
     * Delete the recordingSegment by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete RecordingSegment : {}", id);
        recordingSegmentRepository.deleteById(id);
    }
}
