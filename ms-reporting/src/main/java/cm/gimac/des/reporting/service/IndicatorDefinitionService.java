package cm.gimac.des.reporting.service;

import cm.gimac.des.reporting.domain.IndicatorDefinition;
import cm.gimac.des.reporting.repository.IndicatorDefinitionRepository;
import cm.gimac.des.reporting.service.dto.IndicatorDefinitionDTO;
import cm.gimac.des.reporting.service.mapper.IndicatorDefinitionMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.reporting.domain.IndicatorDefinition}.
 */
@Service
@Transactional
public class IndicatorDefinitionService {

    private static final Logger LOG = LoggerFactory.getLogger(IndicatorDefinitionService.class);

    private final IndicatorDefinitionRepository indicatorDefinitionRepository;

    private final IndicatorDefinitionMapper indicatorDefinitionMapper;

    public IndicatorDefinitionService(
        IndicatorDefinitionRepository indicatorDefinitionRepository,
        IndicatorDefinitionMapper indicatorDefinitionMapper
    ) {
        this.indicatorDefinitionRepository = indicatorDefinitionRepository;
        this.indicatorDefinitionMapper = indicatorDefinitionMapper;
    }

    /**
     * Save a indicatorDefinition.
     *
     * @param indicatorDefinitionDTO the entity to save.
     * @return the persisted entity.
     */
    public IndicatorDefinitionDTO save(IndicatorDefinitionDTO indicatorDefinitionDTO) {
        LOG.debug("Request to save IndicatorDefinition : {}", indicatorDefinitionDTO);
        IndicatorDefinition indicatorDefinition = indicatorDefinitionMapper.toEntity(indicatorDefinitionDTO);
        indicatorDefinition = indicatorDefinitionRepository.save(indicatorDefinition);
        return indicatorDefinitionMapper.toDto(indicatorDefinition);
    }

    /**
     * Update a indicatorDefinition.
     *
     * @param indicatorDefinitionDTO the entity to save.
     * @return the persisted entity.
     */
    public IndicatorDefinitionDTO update(IndicatorDefinitionDTO indicatorDefinitionDTO) {
        LOG.debug("Request to update IndicatorDefinition : {}", indicatorDefinitionDTO);
        IndicatorDefinition indicatorDefinition = indicatorDefinitionMapper.toEntity(indicatorDefinitionDTO);
        indicatorDefinition = indicatorDefinitionRepository.save(indicatorDefinition);
        return indicatorDefinitionMapper.toDto(indicatorDefinition);
    }

    /**
     * Partially update a indicatorDefinition.
     *
     * @param indicatorDefinitionDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<IndicatorDefinitionDTO> partialUpdate(IndicatorDefinitionDTO indicatorDefinitionDTO) {
        LOG.debug("Request to partially update IndicatorDefinition : {}", indicatorDefinitionDTO);

        return indicatorDefinitionRepository
            .findById(indicatorDefinitionDTO.getId())
            .map(existingIndicatorDefinition -> {
                indicatorDefinitionMapper.partialUpdate(existingIndicatorDefinition, indicatorDefinitionDTO);

                return existingIndicatorDefinition;
            })
            .map(indicatorDefinitionRepository::save)
            .map(indicatorDefinitionMapper::toDto);
    }

    /**
     * Get one indicatorDefinition by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<IndicatorDefinitionDTO> findOne(UUID id) {
        LOG.debug("Request to get IndicatorDefinition : {}", id);
        return indicatorDefinitionRepository.findById(id).map(indicatorDefinitionMapper::toDto);
    }

    /**
     * Delete the indicatorDefinition by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete IndicatorDefinition : {}", id);
        indicatorDefinitionRepository.deleteById(id);
    }
}
