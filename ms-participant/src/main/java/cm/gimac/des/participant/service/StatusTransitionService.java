package cm.gimac.des.participant.service;

import cm.gimac.des.participant.domain.StatusTransition;
import cm.gimac.des.participant.repository.StatusTransitionRepository;
import cm.gimac.des.participant.service.dto.StatusTransitionDTO;
import cm.gimac.des.participant.service.mapper.StatusTransitionMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.participant.domain.StatusTransition}.
 */
@Service
@Transactional
public class StatusTransitionService {

    private static final Logger LOG = LoggerFactory.getLogger(StatusTransitionService.class);

    private final StatusTransitionRepository statusTransitionRepository;

    private final StatusTransitionMapper statusTransitionMapper;

    public StatusTransitionService(StatusTransitionRepository statusTransitionRepository, StatusTransitionMapper statusTransitionMapper) {
        this.statusTransitionRepository = statusTransitionRepository;
        this.statusTransitionMapper = statusTransitionMapper;
    }

    /**
     * Save a statusTransition.
     *
     * @param statusTransitionDTO the entity to save.
     * @return the persisted entity.
     */
    public StatusTransitionDTO save(StatusTransitionDTO statusTransitionDTO) {
        LOG.debug("Request to save StatusTransition : {}", statusTransitionDTO);
        StatusTransition statusTransition = statusTransitionMapper.toEntity(statusTransitionDTO);
        statusTransition = statusTransitionRepository.save(statusTransition);
        return statusTransitionMapper.toDto(statusTransition);
    }

    /**
     * Update a statusTransition.
     *
     * @param statusTransitionDTO the entity to save.
     * @return the persisted entity.
     */
    public StatusTransitionDTO update(StatusTransitionDTO statusTransitionDTO) {
        LOG.debug("Request to update StatusTransition : {}", statusTransitionDTO);
        StatusTransition statusTransition = statusTransitionMapper.toEntity(statusTransitionDTO);
        statusTransition = statusTransitionRepository.save(statusTransition);
        return statusTransitionMapper.toDto(statusTransition);
    }

    /**
     * Partially update a statusTransition.
     *
     * @param statusTransitionDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<StatusTransitionDTO> partialUpdate(StatusTransitionDTO statusTransitionDTO) {
        LOG.debug("Request to partially update StatusTransition : {}", statusTransitionDTO);

        return statusTransitionRepository
            .findById(statusTransitionDTO.getId())
            .map(existingStatusTransition -> {
                statusTransitionMapper.partialUpdate(existingStatusTransition, statusTransitionDTO);

                return existingStatusTransition;
            })
            .map(statusTransitionRepository::save)
            .map(statusTransitionMapper::toDto);
    }

    /**
     * Get all the statusTransitions with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<StatusTransitionDTO> findAllWithEagerRelationships(Pageable pageable) {
        return statusTransitionRepository.findAllWithEagerRelationships(pageable).map(statusTransitionMapper::toDto);
    }

    /**
     * Get one statusTransition by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<StatusTransitionDTO> findOne(UUID id) {
        LOG.debug("Request to get StatusTransition : {}", id);
        return statusTransitionRepository.findOneWithEagerRelationships(id).map(statusTransitionMapper::toDto);
    }

    /**
     * Delete the statusTransition by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete StatusTransition : {}", id);
        statusTransitionRepository.deleteById(id);
    }
}
