package cm.gimac.des.monitoring.service;

import cm.gimac.des.monitoring.domain.Alert;
import cm.gimac.des.monitoring.repository.AlertRepository;
import cm.gimac.des.monitoring.service.dto.AlertDTO;
import cm.gimac.des.monitoring.service.mapper.AlertMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.monitoring.domain.Alert}.
 */
@Service
@Transactional
public class AlertService {

    private static final Logger LOG = LoggerFactory.getLogger(AlertService.class);

    private final AlertRepository alertRepository;

    private final AlertMapper alertMapper;

    public AlertService(AlertRepository alertRepository, AlertMapper alertMapper) {
        this.alertRepository = alertRepository;
        this.alertMapper = alertMapper;
    }

    /**
     * Save a alert.
     *
     * @param alertDTO the entity to save.
     * @return the persisted entity.
     */
    public AlertDTO save(AlertDTO alertDTO) {
        LOG.debug("Request to save Alert : {}", alertDTO);
        Alert alert = alertMapper.toEntity(alertDTO);
        alert = alertRepository.save(alert);
        return alertMapper.toDto(alert);
    }

    /**
     * Update a alert.
     *
     * @param alertDTO the entity to save.
     * @return the persisted entity.
     */
    public AlertDTO update(AlertDTO alertDTO) {
        LOG.debug("Request to update Alert : {}", alertDTO);
        Alert alert = alertMapper.toEntity(alertDTO);
        alert = alertRepository.save(alert);
        return alertMapper.toDto(alert);
    }

    /**
     * Partially update a alert.
     *
     * @param alertDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<AlertDTO> partialUpdate(AlertDTO alertDTO) {
        LOG.debug("Request to partially update Alert : {}", alertDTO);

        return alertRepository
            .findById(alertDTO.getId())
            .map(existingAlert -> {
                alertMapper.partialUpdate(existingAlert, alertDTO);

                return existingAlert;
            })
            .map(alertRepository::save)
            .map(alertMapper::toDto);
    }

    /**
     * Get all the alerts with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<AlertDTO> findAllWithEagerRelationships(Pageable pageable) {
        return alertRepository.findAllWithEagerRelationships(pageable).map(alertMapper::toDto);
    }

    /**
     * Get one alert by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<AlertDTO> findOne(UUID id) {
        LOG.debug("Request to get Alert : {}", id);
        return alertRepository.findOneWithEagerRelationships(id).map(alertMapper::toDto);
    }

    /**
     * Delete the alert by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete Alert : {}", id);
        alertRepository.deleteById(id);
    }
}
