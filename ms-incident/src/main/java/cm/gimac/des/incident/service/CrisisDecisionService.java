package cm.gimac.des.incident.service;

import cm.gimac.des.incident.domain.CrisisDecision;
import cm.gimac.des.incident.repository.CrisisDecisionRepository;
import cm.gimac.des.incident.service.dto.CrisisDecisionDTO;
import cm.gimac.des.incident.service.mapper.CrisisDecisionMapper;
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
 * Service Implementation for managing {@link cm.gimac.des.incident.domain.CrisisDecision}.
 */
@Service
@Transactional
public class CrisisDecisionService {

    private static final Logger LOG = LoggerFactory.getLogger(CrisisDecisionService.class);

    private final CrisisDecisionRepository crisisDecisionRepository;

    private final CrisisDecisionMapper crisisDecisionMapper;

    public CrisisDecisionService(CrisisDecisionRepository crisisDecisionRepository, CrisisDecisionMapper crisisDecisionMapper) {
        this.crisisDecisionRepository = crisisDecisionRepository;
        this.crisisDecisionMapper = crisisDecisionMapper;
    }

    /**
     * Save a crisisDecision.
     *
     * @param crisisDecisionDTO the entity to save.
     * @return the persisted entity.
     */
    public CrisisDecisionDTO save(CrisisDecisionDTO crisisDecisionDTO) {
        LOG.debug("Request to save CrisisDecision : {}", crisisDecisionDTO);
        CrisisDecision crisisDecision = crisisDecisionMapper.toEntity(crisisDecisionDTO);
        crisisDecision = crisisDecisionRepository.save(crisisDecision);
        return crisisDecisionMapper.toDto(crisisDecision);
    }

    /**
     * Update a crisisDecision.
     *
     * @param crisisDecisionDTO the entity to save.
     * @return the persisted entity.
     */
    public CrisisDecisionDTO update(CrisisDecisionDTO crisisDecisionDTO) {
        LOG.debug("Request to update CrisisDecision : {}", crisisDecisionDTO);
        CrisisDecision crisisDecision = crisisDecisionMapper.toEntity(crisisDecisionDTO);
        crisisDecision = crisisDecisionRepository.save(crisisDecision);
        return crisisDecisionMapper.toDto(crisisDecision);
    }

    /**
     * Partially update a crisisDecision.
     *
     * @param crisisDecisionDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<CrisisDecisionDTO> partialUpdate(CrisisDecisionDTO crisisDecisionDTO) {
        LOG.debug("Request to partially update CrisisDecision : {}", crisisDecisionDTO);

        return crisisDecisionRepository
            .findById(crisisDecisionDTO.getId())
            .map(existingCrisisDecision -> {
                crisisDecisionMapper.partialUpdate(existingCrisisDecision, crisisDecisionDTO);

                return existingCrisisDecision;
            })
            .map(crisisDecisionRepository::save)
            .map(crisisDecisionMapper::toDto);
    }

    /**
     * Get all the crisisDecisions.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<CrisisDecisionDTO> findAll() {
        LOG.debug("Request to get all CrisisDecisions");
        return crisisDecisionRepository
            .findAll()
            .stream()
            .map(crisisDecisionMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all the crisisDecisions with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<CrisisDecisionDTO> findAllWithEagerRelationships(Pageable pageable) {
        return crisisDecisionRepository.findAllWithEagerRelationships(pageable).map(crisisDecisionMapper::toDto);
    }

    /**
     * Get one crisisDecision by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<CrisisDecisionDTO> findOne(UUID id) {
        LOG.debug("Request to get CrisisDecision : {}", id);
        return crisisDecisionRepository.findOneWithEagerRelationships(id).map(crisisDecisionMapper::toDto);
    }

    /**
     * Delete the crisisDecision by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete CrisisDecision : {}", id);
        crisisDecisionRepository.deleteById(id);
    }
}
