package cm.gimac.des.monitoring.service;

import cm.gimac.des.monitoring.domain.AlertSource;
import cm.gimac.des.monitoring.repository.AlertSourceRepository;
import cm.gimac.des.monitoring.service.dto.AlertSourceDTO;
import cm.gimac.des.monitoring.service.mapper.AlertSourceMapper;
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
 * Service Implementation for managing {@link cm.gimac.des.monitoring.domain.AlertSource}.
 */
@Service
@Transactional
public class AlertSourceService {

    private static final Logger LOG = LoggerFactory.getLogger(AlertSourceService.class);

    private final AlertSourceRepository alertSourceRepository;

    private final AlertSourceMapper alertSourceMapper;

    public AlertSourceService(AlertSourceRepository alertSourceRepository, AlertSourceMapper alertSourceMapper) {
        this.alertSourceRepository = alertSourceRepository;
        this.alertSourceMapper = alertSourceMapper;
    }

    /**
     * Save a alertSource.
     *
     * @param alertSourceDTO the entity to save.
     * @return the persisted entity.
     */
    public AlertSourceDTO save(AlertSourceDTO alertSourceDTO) {
        LOG.debug("Request to save AlertSource : {}", alertSourceDTO);
        AlertSource alertSource = alertSourceMapper.toEntity(alertSourceDTO);
        alertSource = alertSourceRepository.save(alertSource);
        return alertSourceMapper.toDto(alertSource);
    }

    /**
     * Update a alertSource.
     *
     * @param alertSourceDTO the entity to save.
     * @return the persisted entity.
     */
    public AlertSourceDTO update(AlertSourceDTO alertSourceDTO) {
        LOG.debug("Request to update AlertSource : {}", alertSourceDTO);
        AlertSource alertSource = alertSourceMapper.toEntity(alertSourceDTO);
        alertSource = alertSourceRepository.save(alertSource);
        return alertSourceMapper.toDto(alertSource);
    }

    /**
     * Partially update a alertSource.
     *
     * @param alertSourceDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<AlertSourceDTO> partialUpdate(AlertSourceDTO alertSourceDTO) {
        LOG.debug("Request to partially update AlertSource : {}", alertSourceDTO);

        return alertSourceRepository
            .findById(alertSourceDTO.getId())
            .map(existingAlertSource -> {
                alertSourceMapper.partialUpdate(existingAlertSource, alertSourceDTO);

                return existingAlertSource;
            })
            .map(alertSourceRepository::save)
            .map(alertSourceMapper::toDto);
    }

    /**
     * Get all the alertSources.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<AlertSourceDTO> findAll() {
        LOG.debug("Request to get all AlertSources");
        return alertSourceRepository.findAll().stream().map(alertSourceMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one alertSource by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<AlertSourceDTO> findOne(UUID id) {
        LOG.debug("Request to get AlertSource : {}", id);
        return alertSourceRepository.findById(id).map(alertSourceMapper::toDto);
    }

    /**
     * Delete the alertSource by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete AlertSource : {}", id);
        alertSourceRepository.deleteById(id);
    }
}
