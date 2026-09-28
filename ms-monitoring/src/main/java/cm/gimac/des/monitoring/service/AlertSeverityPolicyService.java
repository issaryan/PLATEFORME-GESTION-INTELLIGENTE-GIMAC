package cm.gimac.des.monitoring.service;

import cm.gimac.des.monitoring.domain.AlertSeverityPolicy;
import cm.gimac.des.monitoring.repository.AlertSeverityPolicyRepository;
import cm.gimac.des.monitoring.service.dto.AlertSeverityPolicyDTO;
import cm.gimac.des.monitoring.service.mapper.AlertSeverityPolicyMapper;
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
 * Service Implementation for managing {@link cm.gimac.des.monitoring.domain.AlertSeverityPolicy}.
 */
@Service
@Transactional
public class AlertSeverityPolicyService {

    private static final Logger LOG = LoggerFactory.getLogger(AlertSeverityPolicyService.class);

    private final AlertSeverityPolicyRepository alertSeverityPolicyRepository;

    private final AlertSeverityPolicyMapper alertSeverityPolicyMapper;

    public AlertSeverityPolicyService(
        AlertSeverityPolicyRepository alertSeverityPolicyRepository,
        AlertSeverityPolicyMapper alertSeverityPolicyMapper
    ) {
        this.alertSeverityPolicyRepository = alertSeverityPolicyRepository;
        this.alertSeverityPolicyMapper = alertSeverityPolicyMapper;
    }

    /**
     * Save a alertSeverityPolicy.
     *
     * @param alertSeverityPolicyDTO the entity to save.
     * @return the persisted entity.
     */
    public AlertSeverityPolicyDTO save(AlertSeverityPolicyDTO alertSeverityPolicyDTO) {
        LOG.debug("Request to save AlertSeverityPolicy : {}", alertSeverityPolicyDTO);
        AlertSeverityPolicy alertSeverityPolicy = alertSeverityPolicyMapper.toEntity(alertSeverityPolicyDTO);
        alertSeverityPolicy = alertSeverityPolicyRepository.save(alertSeverityPolicy);
        return alertSeverityPolicyMapper.toDto(alertSeverityPolicy);
    }

    /**
     * Update a alertSeverityPolicy.
     *
     * @param alertSeverityPolicyDTO the entity to save.
     * @return the persisted entity.
     */
    public AlertSeverityPolicyDTO update(AlertSeverityPolicyDTO alertSeverityPolicyDTO) {
        LOG.debug("Request to update AlertSeverityPolicy : {}", alertSeverityPolicyDTO);
        AlertSeverityPolicy alertSeverityPolicy = alertSeverityPolicyMapper.toEntity(alertSeverityPolicyDTO);
        alertSeverityPolicy = alertSeverityPolicyRepository.save(alertSeverityPolicy);
        return alertSeverityPolicyMapper.toDto(alertSeverityPolicy);
    }

    /**
     * Partially update a alertSeverityPolicy.
     *
     * @param alertSeverityPolicyDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<AlertSeverityPolicyDTO> partialUpdate(AlertSeverityPolicyDTO alertSeverityPolicyDTO) {
        LOG.debug("Request to partially update AlertSeverityPolicy : {}", alertSeverityPolicyDTO);

        return alertSeverityPolicyRepository
            .findById(alertSeverityPolicyDTO.getId())
            .map(existingAlertSeverityPolicy -> {
                alertSeverityPolicyMapper.partialUpdate(existingAlertSeverityPolicy, alertSeverityPolicyDTO);

                return existingAlertSeverityPolicy;
            })
            .map(alertSeverityPolicyRepository::save)
            .map(alertSeverityPolicyMapper::toDto);
    }

    /**
     * Get all the alertSeverityPolicies.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<AlertSeverityPolicyDTO> findAll() {
        LOG.debug("Request to get all AlertSeverityPolicies");
        return alertSeverityPolicyRepository
            .findAll()
            .stream()
            .map(alertSeverityPolicyMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one alertSeverityPolicy by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<AlertSeverityPolicyDTO> findOne(UUID id) {
        LOG.debug("Request to get AlertSeverityPolicy : {}", id);
        return alertSeverityPolicyRepository.findById(id).map(alertSeverityPolicyMapper::toDto);
    }

    /**
     * Delete the alertSeverityPolicy by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete AlertSeverityPolicy : {}", id);
        alertSeverityPolicyRepository.deleteById(id);
    }
}
