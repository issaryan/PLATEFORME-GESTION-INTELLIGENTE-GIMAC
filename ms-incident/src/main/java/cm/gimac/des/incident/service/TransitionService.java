package cm.gimac.des.incident.service;

import cm.gimac.des.incident.domain.Transition;
import cm.gimac.des.incident.repository.TransitionRepository;
import cm.gimac.des.incident.service.dto.TransitionDTO;
import cm.gimac.des.incident.service.mapper.TransitionMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.incident.domain.Transition}.
 */
@Service
@Transactional
public class TransitionService {

    private static final Logger LOG = LoggerFactory.getLogger(TransitionService.class);

    private final TransitionRepository transitionRepository;

    private final TransitionMapper transitionMapper;

    public TransitionService(TransitionRepository transitionRepository, TransitionMapper transitionMapper) {
        this.transitionRepository = transitionRepository;
        this.transitionMapper = transitionMapper;
    }

    /**
     * Save a transition.
     *
     * @param transitionDTO the entity to save.
     * @return the persisted entity.
     */
    public TransitionDTO save(TransitionDTO transitionDTO) {
        LOG.debug("Request to save Transition : {}", transitionDTO);
        Transition transition = transitionMapper.toEntity(transitionDTO);
        transition = transitionRepository.save(transition);
        return transitionMapper.toDto(transition);
    }

    /**
     * Update a transition.
     *
     * @param transitionDTO the entity to save.
     * @return the persisted entity.
     */
    public TransitionDTO update(TransitionDTO transitionDTO) {
        LOG.debug("Request to update Transition : {}", transitionDTO);
        Transition transition = transitionMapper.toEntity(transitionDTO);
        transition = transitionRepository.save(transition);
        return transitionMapper.toDto(transition);
    }

    /**
     * Partially update a transition.
     *
     * @param transitionDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<TransitionDTO> partialUpdate(TransitionDTO transitionDTO) {
        LOG.debug("Request to partially update Transition : {}", transitionDTO);

        return transitionRepository
            .findById(transitionDTO.getId())
            .map(existingTransition -> {
                transitionMapper.partialUpdate(existingTransition, transitionDTO);

                return existingTransition;
            })
            .map(transitionRepository::save)
            .map(transitionMapper::toDto);
    }

    /**
     * Get all the transitions with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<TransitionDTO> findAllWithEagerRelationships(Pageable pageable) {
        return transitionRepository.findAllWithEagerRelationships(pageable).map(transitionMapper::toDto);
    }

    /**
     * Get one transition by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<TransitionDTO> findOne(UUID id) {
        LOG.debug("Request to get Transition : {}", id);
        return transitionRepository.findOneWithEagerRelationships(id).map(transitionMapper::toDto);
    }

    /**
     * Delete the transition by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete Transition : {}", id);
        transitionRepository.deleteById(id);
    }
}
