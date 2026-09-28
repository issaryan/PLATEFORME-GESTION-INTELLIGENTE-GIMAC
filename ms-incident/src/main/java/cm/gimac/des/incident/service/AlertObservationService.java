package cm.gimac.des.incident.service;

import cm.gimac.des.incident.domain.AlertObservation;
import cm.gimac.des.incident.repository.AlertObservationRepository;
import cm.gimac.des.incident.service.dto.AlertObservationDTO;
import cm.gimac.des.incident.service.mapper.AlertObservationMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.incident.domain.AlertObservation}.
 */
@Service
@Transactional
public class AlertObservationService {

    private static final Logger LOG = LoggerFactory.getLogger(AlertObservationService.class);

    private final AlertObservationRepository alertObservationRepository;

    private final AlertObservationMapper alertObservationMapper;

    public AlertObservationService(AlertObservationRepository alertObservationRepository, AlertObservationMapper alertObservationMapper) {
        this.alertObservationRepository = alertObservationRepository;
        this.alertObservationMapper = alertObservationMapper;
    }

    /**
     * Save a alertObservation.
     *
     * @param alertObservationDTO the entity to save.
     * @return the persisted entity.
     */
    public AlertObservationDTO save(AlertObservationDTO alertObservationDTO) {
        LOG.debug("Request to save AlertObservation : {}", alertObservationDTO);
        AlertObservation alertObservation = alertObservationMapper.toEntity(alertObservationDTO);
        alertObservation = alertObservationRepository.save(alertObservation);
        return alertObservationMapper.toDto(alertObservation);
    }

    /**
     * Update a alertObservation.
     *
     * @param alertObservationDTO the entity to save.
     * @return the persisted entity.
     */
    public AlertObservationDTO update(AlertObservationDTO alertObservationDTO) {
        LOG.debug("Request to update AlertObservation : {}", alertObservationDTO);
        AlertObservation alertObservation = alertObservationMapper.toEntity(alertObservationDTO);
        alertObservation = alertObservationRepository.save(alertObservation);
        return alertObservationMapper.toDto(alertObservation);
    }

    /**
     * Partially update a alertObservation.
     *
     * @param alertObservationDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<AlertObservationDTO> partialUpdate(AlertObservationDTO alertObservationDTO) {
        LOG.debug("Request to partially update AlertObservation : {}", alertObservationDTO);

        return alertObservationRepository
            .findById(alertObservationDTO.getId())
            .map(existingAlertObservation -> {
                alertObservationMapper.partialUpdate(existingAlertObservation, alertObservationDTO);

                return existingAlertObservation;
            })
            .map(alertObservationRepository::save)
            .map(alertObservationMapper::toDto);
    }

    /**
     * Get all the alertObservations.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<AlertObservationDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all AlertObservations");
        return alertObservationRepository.findAll(pageable).map(alertObservationMapper::toDto);
    }

    /**
     * Get one alertObservation by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<AlertObservationDTO> findOne(UUID id) {
        LOG.debug("Request to get AlertObservation : {}", id);
        return alertObservationRepository.findById(id).map(alertObservationMapper::toDto);
    }

    /**
     * Delete the alertObservation by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete AlertObservation : {}", id);
        alertObservationRepository.deleteById(id);
    }
}
