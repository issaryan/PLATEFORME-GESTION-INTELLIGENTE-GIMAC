package cm.gimac.des.reporting.service;

import cm.gimac.des.reporting.domain.ProjectionRule;
import cm.gimac.des.reporting.repository.ProjectionRuleRepository;
import cm.gimac.des.reporting.service.dto.ProjectionRuleDTO;
import cm.gimac.des.reporting.service.mapper.ProjectionRuleMapper;
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
 * Service Implementation for managing {@link cm.gimac.des.reporting.domain.ProjectionRule}.
 */
@Service
@Transactional
public class ProjectionRuleService {

    private static final Logger LOG = LoggerFactory.getLogger(ProjectionRuleService.class);

    private final ProjectionRuleRepository projectionRuleRepository;

    private final ProjectionRuleMapper projectionRuleMapper;

    public ProjectionRuleService(ProjectionRuleRepository projectionRuleRepository, ProjectionRuleMapper projectionRuleMapper) {
        this.projectionRuleRepository = projectionRuleRepository;
        this.projectionRuleMapper = projectionRuleMapper;
    }

    /**
     * Save a projectionRule.
     *
     * @param projectionRuleDTO the entity to save.
     * @return the persisted entity.
     */
    public ProjectionRuleDTO save(ProjectionRuleDTO projectionRuleDTO) {
        LOG.debug("Request to save ProjectionRule : {}", projectionRuleDTO);
        ProjectionRule projectionRule = projectionRuleMapper.toEntity(projectionRuleDTO);
        projectionRule = projectionRuleRepository.save(projectionRule);
        return projectionRuleMapper.toDto(projectionRule);
    }

    /**
     * Update a projectionRule.
     *
     * @param projectionRuleDTO the entity to save.
     * @return the persisted entity.
     */
    public ProjectionRuleDTO update(ProjectionRuleDTO projectionRuleDTO) {
        LOG.debug("Request to update ProjectionRule : {}", projectionRuleDTO);
        ProjectionRule projectionRule = projectionRuleMapper.toEntity(projectionRuleDTO);
        projectionRule = projectionRuleRepository.save(projectionRule);
        return projectionRuleMapper.toDto(projectionRule);
    }

    /**
     * Partially update a projectionRule.
     *
     * @param projectionRuleDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<ProjectionRuleDTO> partialUpdate(ProjectionRuleDTO projectionRuleDTO) {
        LOG.debug("Request to partially update ProjectionRule : {}", projectionRuleDTO);

        return projectionRuleRepository
            .findById(projectionRuleDTO.getId())
            .map(existingProjectionRule -> {
                projectionRuleMapper.partialUpdate(existingProjectionRule, projectionRuleDTO);

                return existingProjectionRule;
            })
            .map(projectionRuleRepository::save)
            .map(projectionRuleMapper::toDto);
    }

    /**
     * Get all the projectionRules.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<ProjectionRuleDTO> findAll() {
        LOG.debug("Request to get all ProjectionRules");
        return projectionRuleRepository
            .findAll()
            .stream()
            .map(projectionRuleMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one projectionRule by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<ProjectionRuleDTO> findOne(UUID id) {
        LOG.debug("Request to get ProjectionRule : {}", id);
        return projectionRuleRepository.findById(id).map(projectionRuleMapper::toDto);
    }

    /**
     * Delete the projectionRule by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete ProjectionRule : {}", id);
        projectionRuleRepository.deleteById(id);
    }
}
