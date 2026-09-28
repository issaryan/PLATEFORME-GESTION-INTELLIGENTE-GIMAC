package cm.gimac.des.monitoring.service;

import cm.gimac.des.monitoring.domain.OperatorUnavailability;
import cm.gimac.des.monitoring.repository.OperatorUnavailabilityRepository;
import cm.gimac.des.monitoring.service.dto.OperatorUnavailabilityDTO;
import cm.gimac.des.monitoring.service.mapper.OperatorUnavailabilityMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.monitoring.domain.OperatorUnavailability}.
 */
@Service
@Transactional
public class OperatorUnavailabilityService {

    private static final Logger LOG = LoggerFactory.getLogger(OperatorUnavailabilityService.class);

    private final OperatorUnavailabilityRepository operatorUnavailabilityRepository;

    private final OperatorUnavailabilityMapper operatorUnavailabilityMapper;

    public OperatorUnavailabilityService(
        OperatorUnavailabilityRepository operatorUnavailabilityRepository,
        OperatorUnavailabilityMapper operatorUnavailabilityMapper
    ) {
        this.operatorUnavailabilityRepository = operatorUnavailabilityRepository;
        this.operatorUnavailabilityMapper = operatorUnavailabilityMapper;
    }

    /**
     * Save a operatorUnavailability.
     *
     * @param operatorUnavailabilityDTO the entity to save.
     * @return the persisted entity.
     */
    public OperatorUnavailabilityDTO save(OperatorUnavailabilityDTO operatorUnavailabilityDTO) {
        LOG.debug("Request to save OperatorUnavailability : {}", operatorUnavailabilityDTO);
        OperatorUnavailability operatorUnavailability = operatorUnavailabilityMapper.toEntity(operatorUnavailabilityDTO);
        operatorUnavailability = operatorUnavailabilityRepository.save(operatorUnavailability);
        return operatorUnavailabilityMapper.toDto(operatorUnavailability);
    }

    /**
     * Update a operatorUnavailability.
     *
     * @param operatorUnavailabilityDTO the entity to save.
     * @return the persisted entity.
     */
    public OperatorUnavailabilityDTO update(OperatorUnavailabilityDTO operatorUnavailabilityDTO) {
        LOG.debug("Request to update OperatorUnavailability : {}", operatorUnavailabilityDTO);
        OperatorUnavailability operatorUnavailability = operatorUnavailabilityMapper.toEntity(operatorUnavailabilityDTO);
        operatorUnavailability = operatorUnavailabilityRepository.save(operatorUnavailability);
        return operatorUnavailabilityMapper.toDto(operatorUnavailability);
    }

    /**
     * Partially update a operatorUnavailability.
     *
     * @param operatorUnavailabilityDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<OperatorUnavailabilityDTO> partialUpdate(OperatorUnavailabilityDTO operatorUnavailabilityDTO) {
        LOG.debug("Request to partially update OperatorUnavailability : {}", operatorUnavailabilityDTO);

        return operatorUnavailabilityRepository
            .findById(operatorUnavailabilityDTO.getId())
            .map(existingOperatorUnavailability -> {
                operatorUnavailabilityMapper.partialUpdate(existingOperatorUnavailability, operatorUnavailabilityDTO);

                return existingOperatorUnavailability;
            })
            .map(operatorUnavailabilityRepository::save)
            .map(operatorUnavailabilityMapper::toDto);
    }

    /**
     * Get one operatorUnavailability by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<OperatorUnavailabilityDTO> findOne(UUID id) {
        LOG.debug("Request to get OperatorUnavailability : {}", id);
        return operatorUnavailabilityRepository.findById(id).map(operatorUnavailabilityMapper::toDto);
    }

    /**
     * Delete the operatorUnavailability by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete OperatorUnavailability : {}", id);
        operatorUnavailabilityRepository.deleteById(id);
    }
}
