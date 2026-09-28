package cm.gimac.des.pilotage.service;

import cm.gimac.des.pilotage.domain.Decision;
import cm.gimac.des.pilotage.repository.DecisionRepository;
import cm.gimac.des.pilotage.service.dto.DecisionDTO;
import cm.gimac.des.pilotage.service.mapper.DecisionMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.pilotage.domain.Decision}.
 */
@Service
@Transactional
public class DecisionService {

    private static final Logger LOG = LoggerFactory.getLogger(DecisionService.class);

    private final DecisionRepository decisionRepository;

    private final DecisionMapper decisionMapper;

    public DecisionService(DecisionRepository decisionRepository, DecisionMapper decisionMapper) {
        this.decisionRepository = decisionRepository;
        this.decisionMapper = decisionMapper;
    }

    /**
     * Save a decision.
     *
     * @param decisionDTO the entity to save.
     * @return the persisted entity.
     */
    public DecisionDTO save(DecisionDTO decisionDTO) {
        LOG.debug("Request to save Decision : {}", decisionDTO);
        Decision decision = decisionMapper.toEntity(decisionDTO);
        decision = decisionRepository.save(decision);
        return decisionMapper.toDto(decision);
    }

    /**
     * Update a decision.
     *
     * @param decisionDTO the entity to save.
     * @return the persisted entity.
     */
    public DecisionDTO update(DecisionDTO decisionDTO) {
        LOG.debug("Request to update Decision : {}", decisionDTO);
        Decision decision = decisionMapper.toEntity(decisionDTO);
        decision = decisionRepository.save(decision);
        return decisionMapper.toDto(decision);
    }

    /**
     * Partially update a decision.
     *
     * @param decisionDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<DecisionDTO> partialUpdate(DecisionDTO decisionDTO) {
        LOG.debug("Request to partially update Decision : {}", decisionDTO);

        return decisionRepository
            .findById(decisionDTO.getId())
            .map(existingDecision -> {
                decisionMapper.partialUpdate(existingDecision, decisionDTO);

                return existingDecision;
            })
            .map(decisionRepository::save)
            .map(decisionMapper::toDto);
    }

    /**
     * Get all the decisions with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<DecisionDTO> findAllWithEagerRelationships(Pageable pageable) {
        return decisionRepository.findAllWithEagerRelationships(pageable).map(decisionMapper::toDto);
    }

    /**
     * Get one decision by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<DecisionDTO> findOne(UUID id) {
        LOG.debug("Request to get Decision : {}", id);
        return decisionRepository.findOneWithEagerRelationships(id).map(decisionMapper::toDto);
    }

    /**
     * Delete the decision by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete Decision : {}", id);
        decisionRepository.deleteById(id);
    }
}
