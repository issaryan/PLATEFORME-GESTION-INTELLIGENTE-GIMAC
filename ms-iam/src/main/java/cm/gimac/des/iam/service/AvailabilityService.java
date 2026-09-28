package cm.gimac.des.iam.service;

import cm.gimac.des.iam.domain.Availability;
import cm.gimac.des.iam.repository.AvailabilityRepository;
import cm.gimac.des.iam.service.dto.AvailabilityDTO;
import cm.gimac.des.iam.service.mapper.AvailabilityMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.iam.domain.Availability}.
 */
@Service
@Transactional
public class AvailabilityService {

    private static final Logger LOG = LoggerFactory.getLogger(AvailabilityService.class);

    private final AvailabilityRepository availabilityRepository;

    private final AvailabilityMapper availabilityMapper;

    public AvailabilityService(AvailabilityRepository availabilityRepository, AvailabilityMapper availabilityMapper) {
        this.availabilityRepository = availabilityRepository;
        this.availabilityMapper = availabilityMapper;
    }

    /**
     * Save a availability.
     *
     * @param availabilityDTO the entity to save.
     * @return the persisted entity.
     */
    public AvailabilityDTO save(AvailabilityDTO availabilityDTO) {
        LOG.debug("Request to save Availability : {}", availabilityDTO);
        Availability availability = availabilityMapper.toEntity(availabilityDTO);
        availability = availabilityRepository.save(availability);
        return availabilityMapper.toDto(availability);
    }

    /**
     * Update a availability.
     *
     * @param availabilityDTO the entity to save.
     * @return the persisted entity.
     */
    public AvailabilityDTO update(AvailabilityDTO availabilityDTO) {
        LOG.debug("Request to update Availability : {}", availabilityDTO);
        Availability availability = availabilityMapper.toEntity(availabilityDTO);
        availability = availabilityRepository.save(availability);
        return availabilityMapper.toDto(availability);
    }

    /**
     * Partially update a availability.
     *
     * @param availabilityDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<AvailabilityDTO> partialUpdate(AvailabilityDTO availabilityDTO) {
        LOG.debug("Request to partially update Availability : {}", availabilityDTO);

        return availabilityRepository
            .findById(availabilityDTO.getId())
            .map(existingAvailability -> {
                availabilityMapper.partialUpdate(existingAvailability, availabilityDTO);

                return existingAvailability;
            })
            .map(availabilityRepository::save)
            .map(availabilityMapper::toDto);
    }

    /**
     * Get all the availabilities with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<AvailabilityDTO> findAllWithEagerRelationships(Pageable pageable) {
        return availabilityRepository.findAllWithEagerRelationships(pageable).map(availabilityMapper::toDto);
    }

    /**
     * Get one availability by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<AvailabilityDTO> findOne(UUID id) {
        LOG.debug("Request to get Availability : {}", id);
        return availabilityRepository.findOneWithEagerRelationships(id).map(availabilityMapper::toDto);
    }

    /**
     * Delete the availability by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete Availability : {}", id);
        availabilityRepository.deleteById(id);
    }
}
