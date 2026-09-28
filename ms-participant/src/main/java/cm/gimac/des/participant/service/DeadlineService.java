package cm.gimac.des.participant.service;

import cm.gimac.des.participant.domain.Deadline;
import cm.gimac.des.participant.repository.DeadlineRepository;
import cm.gimac.des.participant.service.dto.DeadlineDTO;
import cm.gimac.des.participant.service.mapper.DeadlineMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.participant.domain.Deadline}.
 */
@Service
@Transactional
public class DeadlineService {

    private static final Logger LOG = LoggerFactory.getLogger(DeadlineService.class);

    private final DeadlineRepository deadlineRepository;

    private final DeadlineMapper deadlineMapper;

    public DeadlineService(DeadlineRepository deadlineRepository, DeadlineMapper deadlineMapper) {
        this.deadlineRepository = deadlineRepository;
        this.deadlineMapper = deadlineMapper;
    }

    /**
     * Save a deadline.
     *
     * @param deadlineDTO the entity to save.
     * @return the persisted entity.
     */
    public DeadlineDTO save(DeadlineDTO deadlineDTO) {
        LOG.debug("Request to save Deadline : {}", deadlineDTO);
        Deadline deadline = deadlineMapper.toEntity(deadlineDTO);
        deadline = deadlineRepository.save(deadline);
        return deadlineMapper.toDto(deadline);
    }

    /**
     * Update a deadline.
     *
     * @param deadlineDTO the entity to save.
     * @return the persisted entity.
     */
    public DeadlineDTO update(DeadlineDTO deadlineDTO) {
        LOG.debug("Request to update Deadline : {}", deadlineDTO);
        Deadline deadline = deadlineMapper.toEntity(deadlineDTO);
        deadline = deadlineRepository.save(deadline);
        return deadlineMapper.toDto(deadline);
    }

    /**
     * Partially update a deadline.
     *
     * @param deadlineDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<DeadlineDTO> partialUpdate(DeadlineDTO deadlineDTO) {
        LOG.debug("Request to partially update Deadline : {}", deadlineDTO);

        return deadlineRepository
            .findById(deadlineDTO.getId())
            .map(existingDeadline -> {
                deadlineMapper.partialUpdate(existingDeadline, deadlineDTO);

                return existingDeadline;
            })
            .map(deadlineRepository::save)
            .map(deadlineMapper::toDto);
    }

    /**
     * Get all the deadlines with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<DeadlineDTO> findAllWithEagerRelationships(Pageable pageable) {
        return deadlineRepository.findAllWithEagerRelationships(pageable).map(deadlineMapper::toDto);
    }

    /**
     * Get one deadline by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<DeadlineDTO> findOne(UUID id) {
        LOG.debug("Request to get Deadline : {}", id);
        return deadlineRepository.findOneWithEagerRelationships(id).map(deadlineMapper::toDto);
    }

    /**
     * Delete the deadline by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete Deadline : {}", id);
        deadlineRepository.deleteById(id);
    }
}
