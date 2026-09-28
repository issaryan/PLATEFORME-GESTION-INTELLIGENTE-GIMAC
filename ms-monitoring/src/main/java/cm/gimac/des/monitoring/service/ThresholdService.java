package cm.gimac.des.monitoring.service;

import cm.gimac.des.monitoring.domain.Threshold;
import cm.gimac.des.monitoring.repository.ThresholdRepository;
import cm.gimac.des.monitoring.service.dto.ThresholdDTO;
import cm.gimac.des.monitoring.service.mapper.ThresholdMapper;
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
 * Service Implementation for managing {@link cm.gimac.des.monitoring.domain.Threshold}.
 */
@Service
@Transactional
public class ThresholdService {

    private static final Logger LOG = LoggerFactory.getLogger(ThresholdService.class);

    private final ThresholdRepository thresholdRepository;

    private final ThresholdMapper thresholdMapper;

    public ThresholdService(ThresholdRepository thresholdRepository, ThresholdMapper thresholdMapper) {
        this.thresholdRepository = thresholdRepository;
        this.thresholdMapper = thresholdMapper;
    }

    /**
     * Save a threshold.
     *
     * @param thresholdDTO the entity to save.
     * @return the persisted entity.
     */
    public ThresholdDTO save(ThresholdDTO thresholdDTO) {
        LOG.debug("Request to save Threshold : {}", thresholdDTO);
        Threshold threshold = thresholdMapper.toEntity(thresholdDTO);
        threshold = thresholdRepository.save(threshold);
        return thresholdMapper.toDto(threshold);
    }

    /**
     * Update a threshold.
     *
     * @param thresholdDTO the entity to save.
     * @return the persisted entity.
     */
    public ThresholdDTO update(ThresholdDTO thresholdDTO) {
        LOG.debug("Request to update Threshold : {}", thresholdDTO);
        Threshold threshold = thresholdMapper.toEntity(thresholdDTO);
        threshold = thresholdRepository.save(threshold);
        return thresholdMapper.toDto(threshold);
    }

    /**
     * Partially update a threshold.
     *
     * @param thresholdDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<ThresholdDTO> partialUpdate(ThresholdDTO thresholdDTO) {
        LOG.debug("Request to partially update Threshold : {}", thresholdDTO);

        return thresholdRepository
            .findById(thresholdDTO.getId())
            .map(existingThreshold -> {
                thresholdMapper.partialUpdate(existingThreshold, thresholdDTO);

                return existingThreshold;
            })
            .map(thresholdRepository::save)
            .map(thresholdMapper::toDto);
    }

    /**
     * Get all the thresholds.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<ThresholdDTO> findAll() {
        LOG.debug("Request to get all Thresholds");
        return thresholdRepository.findAll().stream().map(thresholdMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all the thresholds with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<ThresholdDTO> findAllWithEagerRelationships(Pageable pageable) {
        return thresholdRepository.findAllWithEagerRelationships(pageable).map(thresholdMapper::toDto);
    }

    /**
     * Get one threshold by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<ThresholdDTO> findOne(UUID id) {
        LOG.debug("Request to get Threshold : {}", id);
        return thresholdRepository.findOneWithEagerRelationships(id).map(thresholdMapper::toDto);
    }

    /**
     * Delete the threshold by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete Threshold : {}", id);
        thresholdRepository.deleteById(id);
    }
}
