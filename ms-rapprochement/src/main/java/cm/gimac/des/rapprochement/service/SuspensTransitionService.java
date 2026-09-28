package cm.gimac.des.rapprochement.service;

import cm.gimac.des.rapprochement.domain.SuspensTransition;
import cm.gimac.des.rapprochement.repository.SuspensTransitionRepository;
import cm.gimac.des.rapprochement.service.dto.SuspensTransitionDTO;
import cm.gimac.des.rapprochement.service.mapper.SuspensTransitionMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.rapprochement.domain.SuspensTransition}.
 */
@Service
@Transactional
public class SuspensTransitionService {

    private static final Logger LOG = LoggerFactory.getLogger(SuspensTransitionService.class);

    private final SuspensTransitionRepository suspensTransitionRepository;

    private final SuspensTransitionMapper suspensTransitionMapper;

    public SuspensTransitionService(
        SuspensTransitionRepository suspensTransitionRepository,
        SuspensTransitionMapper suspensTransitionMapper
    ) {
        this.suspensTransitionRepository = suspensTransitionRepository;
        this.suspensTransitionMapper = suspensTransitionMapper;
    }

    /**
     * Save a suspensTransition.
     *
     * @param suspensTransitionDTO the entity to save.
     * @return the persisted entity.
     */
    public SuspensTransitionDTO save(SuspensTransitionDTO suspensTransitionDTO) {
        LOG.debug("Request to save SuspensTransition : {}", suspensTransitionDTO);
        SuspensTransition suspensTransition = suspensTransitionMapper.toEntity(suspensTransitionDTO);
        suspensTransition = suspensTransitionRepository.save(suspensTransition);
        return suspensTransitionMapper.toDto(suspensTransition);
    }

    /**
     * Update a suspensTransition.
     *
     * @param suspensTransitionDTO the entity to save.
     * @return the persisted entity.
     */
    public SuspensTransitionDTO update(SuspensTransitionDTO suspensTransitionDTO) {
        LOG.debug("Request to update SuspensTransition : {}", suspensTransitionDTO);
        SuspensTransition suspensTransition = suspensTransitionMapper.toEntity(suspensTransitionDTO);
        suspensTransition = suspensTransitionRepository.save(suspensTransition);
        return suspensTransitionMapper.toDto(suspensTransition);
    }

    /**
     * Partially update a suspensTransition.
     *
     * @param suspensTransitionDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<SuspensTransitionDTO> partialUpdate(SuspensTransitionDTO suspensTransitionDTO) {
        LOG.debug("Request to partially update SuspensTransition : {}", suspensTransitionDTO);

        return suspensTransitionRepository
            .findById(suspensTransitionDTO.getId())
            .map(existingSuspensTransition -> {
                suspensTransitionMapper.partialUpdate(existingSuspensTransition, suspensTransitionDTO);

                return existingSuspensTransition;
            })
            .map(suspensTransitionRepository::save)
            .map(suspensTransitionMapper::toDto);
    }

    /**
     * Get all the suspensTransitions with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<SuspensTransitionDTO> findAllWithEagerRelationships(Pageable pageable) {
        return suspensTransitionRepository.findAllWithEagerRelationships(pageable).map(suspensTransitionMapper::toDto);
    }

    /**
     * Get one suspensTransition by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<SuspensTransitionDTO> findOne(UUID id) {
        LOG.debug("Request to get SuspensTransition : {}", id);
        return suspensTransitionRepository.findOneWithEagerRelationships(id).map(suspensTransitionMapper::toDto);
    }

    /**
     * Delete the suspensTransition by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete SuspensTransition : {}", id);
        suspensTransitionRepository.deleteById(id);
    }
}
