package cm.gimac.des.rapprochement.service;

import cm.gimac.des.rapprochement.domain.AgingRule;
import cm.gimac.des.rapprochement.repository.AgingRuleRepository;
import cm.gimac.des.rapprochement.service.dto.AgingRuleDTO;
import cm.gimac.des.rapprochement.service.mapper.AgingRuleMapper;
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
 * Service Implementation for managing {@link cm.gimac.des.rapprochement.domain.AgingRule}.
 */
@Service
@Transactional
public class AgingRuleService {

    private static final Logger LOG = LoggerFactory.getLogger(AgingRuleService.class);

    private final AgingRuleRepository agingRuleRepository;

    private final AgingRuleMapper agingRuleMapper;

    public AgingRuleService(AgingRuleRepository agingRuleRepository, AgingRuleMapper agingRuleMapper) {
        this.agingRuleRepository = agingRuleRepository;
        this.agingRuleMapper = agingRuleMapper;
    }

    /**
     * Save a agingRule.
     *
     * @param agingRuleDTO the entity to save.
     * @return the persisted entity.
     */
    public AgingRuleDTO save(AgingRuleDTO agingRuleDTO) {
        LOG.debug("Request to save AgingRule : {}", agingRuleDTO);
        AgingRule agingRule = agingRuleMapper.toEntity(agingRuleDTO);
        agingRule = agingRuleRepository.save(agingRule);
        return agingRuleMapper.toDto(agingRule);
    }

    /**
     * Update a agingRule.
     *
     * @param agingRuleDTO the entity to save.
     * @return the persisted entity.
     */
    public AgingRuleDTO update(AgingRuleDTO agingRuleDTO) {
        LOG.debug("Request to update AgingRule : {}", agingRuleDTO);
        AgingRule agingRule = agingRuleMapper.toEntity(agingRuleDTO);
        agingRule = agingRuleRepository.save(agingRule);
        return agingRuleMapper.toDto(agingRule);
    }

    /**
     * Partially update a agingRule.
     *
     * @param agingRuleDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<AgingRuleDTO> partialUpdate(AgingRuleDTO agingRuleDTO) {
        LOG.debug("Request to partially update AgingRule : {}", agingRuleDTO);

        return agingRuleRepository
            .findById(agingRuleDTO.getId())
            .map(existingAgingRule -> {
                agingRuleMapper.partialUpdate(existingAgingRule, agingRuleDTO);

                return existingAgingRule;
            })
            .map(agingRuleRepository::save)
            .map(agingRuleMapper::toDto);
    }

    /**
     * Get all the agingRules.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<AgingRuleDTO> findAll() {
        LOG.debug("Request to get all AgingRules");
        return agingRuleRepository.findAll().stream().map(agingRuleMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one agingRule by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<AgingRuleDTO> findOne(UUID id) {
        LOG.debug("Request to get AgingRule : {}", id);
        return agingRuleRepository.findById(id).map(agingRuleMapper::toDto);
    }

    /**
     * Delete the agingRule by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete AgingRule : {}", id);
        agingRuleRepository.deleteById(id);
    }
}
