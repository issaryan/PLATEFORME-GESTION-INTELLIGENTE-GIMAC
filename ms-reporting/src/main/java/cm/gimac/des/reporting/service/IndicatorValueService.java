package cm.gimac.des.reporting.service;

import cm.gimac.des.reporting.domain.IndicatorValue;
import cm.gimac.des.reporting.repository.IndicatorValueRepository;
import cm.gimac.des.reporting.service.dto.IndicatorValueDTO;
import cm.gimac.des.reporting.service.mapper.IndicatorValueMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.reporting.domain.IndicatorValue}.
 */
@Service
@Transactional
public class IndicatorValueService {

    private static final Logger LOG = LoggerFactory.getLogger(IndicatorValueService.class);

    private final IndicatorValueRepository indicatorValueRepository;

    private final IndicatorValueMapper indicatorValueMapper;

    public IndicatorValueService(IndicatorValueRepository indicatorValueRepository, IndicatorValueMapper indicatorValueMapper) {
        this.indicatorValueRepository = indicatorValueRepository;
        this.indicatorValueMapper = indicatorValueMapper;
    }

    /**
     * Save a indicatorValue.
     *
     * @param indicatorValueDTO the entity to save.
     * @return the persisted entity.
     */
    public IndicatorValueDTO save(IndicatorValueDTO indicatorValueDTO) {
        LOG.debug("Request to save IndicatorValue : {}", indicatorValueDTO);
        IndicatorValue indicatorValue = indicatorValueMapper.toEntity(indicatorValueDTO);
        indicatorValue = indicatorValueRepository.save(indicatorValue);
        return indicatorValueMapper.toDto(indicatorValue);
    }

    /**
     * Update a indicatorValue.
     *
     * @param indicatorValueDTO the entity to save.
     * @return the persisted entity.
     */
    public IndicatorValueDTO update(IndicatorValueDTO indicatorValueDTO) {
        LOG.debug("Request to update IndicatorValue : {}", indicatorValueDTO);
        IndicatorValue indicatorValue = indicatorValueMapper.toEntity(indicatorValueDTO);
        indicatorValue = indicatorValueRepository.save(indicatorValue);
        return indicatorValueMapper.toDto(indicatorValue);
    }

    /**
     * Partially update a indicatorValue.
     *
     * @param indicatorValueDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<IndicatorValueDTO> partialUpdate(IndicatorValueDTO indicatorValueDTO) {
        LOG.debug("Request to partially update IndicatorValue : {}", indicatorValueDTO);

        return indicatorValueRepository
            .findById(indicatorValueDTO.getId())
            .map(existingIndicatorValue -> {
                indicatorValueMapper.partialUpdate(existingIndicatorValue, indicatorValueDTO);

                return existingIndicatorValue;
            })
            .map(indicatorValueRepository::save)
            .map(indicatorValueMapper::toDto);
    }

    /**
     * Get all the indicatorValues with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<IndicatorValueDTO> findAllWithEagerRelationships(Pageable pageable) {
        return indicatorValueRepository.findAllWithEagerRelationships(pageable).map(indicatorValueMapper::toDto);
    }

    /**
     * Get one indicatorValue by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<IndicatorValueDTO> findOne(UUID id) {
        LOG.debug("Request to get IndicatorValue : {}", id);
        return indicatorValueRepository.findOneWithEagerRelationships(id).map(indicatorValueMapper::toDto);
    }

    /**
     * Delete the indicatorValue by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete IndicatorValue : {}", id);
        indicatorValueRepository.deleteById(id);
    }
}
