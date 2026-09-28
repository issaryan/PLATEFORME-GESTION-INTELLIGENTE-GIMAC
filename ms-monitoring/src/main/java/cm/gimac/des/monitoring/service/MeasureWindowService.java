package cm.gimac.des.monitoring.service;

import cm.gimac.des.monitoring.domain.MeasureWindow;
import cm.gimac.des.monitoring.repository.MeasureWindowRepository;
import cm.gimac.des.monitoring.service.dto.MeasureWindowDTO;
import cm.gimac.des.monitoring.service.mapper.MeasureWindowMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.monitoring.domain.MeasureWindow}.
 */
@Service
@Transactional
public class MeasureWindowService {

    private static final Logger LOG = LoggerFactory.getLogger(MeasureWindowService.class);

    private final MeasureWindowRepository measureWindowRepository;

    private final MeasureWindowMapper measureWindowMapper;

    public MeasureWindowService(MeasureWindowRepository measureWindowRepository, MeasureWindowMapper measureWindowMapper) {
        this.measureWindowRepository = measureWindowRepository;
        this.measureWindowMapper = measureWindowMapper;
    }

    /**
     * Save a measureWindow.
     *
     * @param measureWindowDTO the entity to save.
     * @return the persisted entity.
     */
    public MeasureWindowDTO save(MeasureWindowDTO measureWindowDTO) {
        LOG.debug("Request to save MeasureWindow : {}", measureWindowDTO);
        MeasureWindow measureWindow = measureWindowMapper.toEntity(measureWindowDTO);
        measureWindow = measureWindowRepository.save(measureWindow);
        return measureWindowMapper.toDto(measureWindow);
    }

    /**
     * Update a measureWindow.
     *
     * @param measureWindowDTO the entity to save.
     * @return the persisted entity.
     */
    public MeasureWindowDTO update(MeasureWindowDTO measureWindowDTO) {
        LOG.debug("Request to update MeasureWindow : {}", measureWindowDTO);
        MeasureWindow measureWindow = measureWindowMapper.toEntity(measureWindowDTO);
        measureWindow = measureWindowRepository.save(measureWindow);
        return measureWindowMapper.toDto(measureWindow);
    }

    /**
     * Partially update a measureWindow.
     *
     * @param measureWindowDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<MeasureWindowDTO> partialUpdate(MeasureWindowDTO measureWindowDTO) {
        LOG.debug("Request to partially update MeasureWindow : {}", measureWindowDTO);

        return measureWindowRepository
            .findById(measureWindowDTO.getId())
            .map(existingMeasureWindow -> {
                measureWindowMapper.partialUpdate(existingMeasureWindow, measureWindowDTO);

                return existingMeasureWindow;
            })
            .map(measureWindowRepository::save)
            .map(measureWindowMapper::toDto);
    }

    /**
     * Get all the measureWindows with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<MeasureWindowDTO> findAllWithEagerRelationships(Pageable pageable) {
        return measureWindowRepository.findAllWithEagerRelationships(pageable).map(measureWindowMapper::toDto);
    }

    /**
     * Get one measureWindow by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<MeasureWindowDTO> findOne(UUID id) {
        LOG.debug("Request to get MeasureWindow : {}", id);
        return measureWindowRepository.findOneWithEagerRelationships(id).map(measureWindowMapper::toDto);
    }

    /**
     * Delete the measureWindow by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete MeasureWindow : {}", id);
        measureWindowRepository.deleteById(id);
    }
}
