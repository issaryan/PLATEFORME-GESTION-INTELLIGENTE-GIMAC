package cm.gimac.des.monitoring.service;

import cm.gimac.des.monitoring.domain.OnCallAssignment;
import cm.gimac.des.monitoring.repository.OnCallAssignmentRepository;
import cm.gimac.des.monitoring.service.dto.OnCallAssignmentDTO;
import cm.gimac.des.monitoring.service.mapper.OnCallAssignmentMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.monitoring.domain.OnCallAssignment}.
 */
@Service
@Transactional
public class OnCallAssignmentService {

    private static final Logger LOG = LoggerFactory.getLogger(OnCallAssignmentService.class);

    private final OnCallAssignmentRepository onCallAssignmentRepository;

    private final OnCallAssignmentMapper onCallAssignmentMapper;

    public OnCallAssignmentService(OnCallAssignmentRepository onCallAssignmentRepository, OnCallAssignmentMapper onCallAssignmentMapper) {
        this.onCallAssignmentRepository = onCallAssignmentRepository;
        this.onCallAssignmentMapper = onCallAssignmentMapper;
    }

    /**
     * Save a onCallAssignment.
     *
     * @param onCallAssignmentDTO the entity to save.
     * @return the persisted entity.
     */
    public OnCallAssignmentDTO save(OnCallAssignmentDTO onCallAssignmentDTO) {
        LOG.debug("Request to save OnCallAssignment : {}", onCallAssignmentDTO);
        OnCallAssignment onCallAssignment = onCallAssignmentMapper.toEntity(onCallAssignmentDTO);
        onCallAssignment = onCallAssignmentRepository.save(onCallAssignment);
        return onCallAssignmentMapper.toDto(onCallAssignment);
    }

    /**
     * Update a onCallAssignment.
     *
     * @param onCallAssignmentDTO the entity to save.
     * @return the persisted entity.
     */
    public OnCallAssignmentDTO update(OnCallAssignmentDTO onCallAssignmentDTO) {
        LOG.debug("Request to update OnCallAssignment : {}", onCallAssignmentDTO);
        OnCallAssignment onCallAssignment = onCallAssignmentMapper.toEntity(onCallAssignmentDTO);
        onCallAssignment = onCallAssignmentRepository.save(onCallAssignment);
        return onCallAssignmentMapper.toDto(onCallAssignment);
    }

    /**
     * Partially update a onCallAssignment.
     *
     * @param onCallAssignmentDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<OnCallAssignmentDTO> partialUpdate(OnCallAssignmentDTO onCallAssignmentDTO) {
        LOG.debug("Request to partially update OnCallAssignment : {}", onCallAssignmentDTO);

        return onCallAssignmentRepository
            .findById(onCallAssignmentDTO.getId())
            .map(existingOnCallAssignment -> {
                onCallAssignmentMapper.partialUpdate(existingOnCallAssignment, onCallAssignmentDTO);

                return existingOnCallAssignment;
            })
            .map(onCallAssignmentRepository::save)
            .map(onCallAssignmentMapper::toDto);
    }

    /**
     * Get all the onCallAssignments with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<OnCallAssignmentDTO> findAllWithEagerRelationships(Pageable pageable) {
        return onCallAssignmentRepository.findAllWithEagerRelationships(pageable).map(onCallAssignmentMapper::toDto);
    }

    /**
     * Get one onCallAssignment by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<OnCallAssignmentDTO> findOne(UUID id) {
        LOG.debug("Request to get OnCallAssignment : {}", id);
        return onCallAssignmentRepository.findOneWithEagerRelationships(id).map(onCallAssignmentMapper::toDto);
    }

    /**
     * Delete the onCallAssignment by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete OnCallAssignment : {}", id);
        onCallAssignmentRepository.deleteById(id);
    }
}
