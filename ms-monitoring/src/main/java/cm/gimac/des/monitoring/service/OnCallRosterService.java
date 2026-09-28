package cm.gimac.des.monitoring.service;

import cm.gimac.des.monitoring.domain.OnCallRoster;
import cm.gimac.des.monitoring.repository.OnCallRosterRepository;
import cm.gimac.des.monitoring.service.dto.OnCallRosterDTO;
import cm.gimac.des.monitoring.service.mapper.OnCallRosterMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.monitoring.domain.OnCallRoster}.
 */
@Service
@Transactional
public class OnCallRosterService {

    private static final Logger LOG = LoggerFactory.getLogger(OnCallRosterService.class);

    private final OnCallRosterRepository onCallRosterRepository;

    private final OnCallRosterMapper onCallRosterMapper;

    public OnCallRosterService(OnCallRosterRepository onCallRosterRepository, OnCallRosterMapper onCallRosterMapper) {
        this.onCallRosterRepository = onCallRosterRepository;
        this.onCallRosterMapper = onCallRosterMapper;
    }

    /**
     * Save a onCallRoster.
     *
     * @param onCallRosterDTO the entity to save.
     * @return the persisted entity.
     */
    public OnCallRosterDTO save(OnCallRosterDTO onCallRosterDTO) {
        LOG.debug("Request to save OnCallRoster : {}", onCallRosterDTO);
        OnCallRoster onCallRoster = onCallRosterMapper.toEntity(onCallRosterDTO);
        onCallRoster = onCallRosterRepository.save(onCallRoster);
        return onCallRosterMapper.toDto(onCallRoster);
    }

    /**
     * Update a onCallRoster.
     *
     * @param onCallRosterDTO the entity to save.
     * @return the persisted entity.
     */
    public OnCallRosterDTO update(OnCallRosterDTO onCallRosterDTO) {
        LOG.debug("Request to update OnCallRoster : {}", onCallRosterDTO);
        OnCallRoster onCallRoster = onCallRosterMapper.toEntity(onCallRosterDTO);
        onCallRoster = onCallRosterRepository.save(onCallRoster);
        return onCallRosterMapper.toDto(onCallRoster);
    }

    /**
     * Partially update a onCallRoster.
     *
     * @param onCallRosterDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<OnCallRosterDTO> partialUpdate(OnCallRosterDTO onCallRosterDTO) {
        LOG.debug("Request to partially update OnCallRoster : {}", onCallRosterDTO);

        return onCallRosterRepository
            .findById(onCallRosterDTO.getId())
            .map(existingOnCallRoster -> {
                onCallRosterMapper.partialUpdate(existingOnCallRoster, onCallRosterDTO);

                return existingOnCallRoster;
            })
            .map(onCallRosterRepository::save)
            .map(onCallRosterMapper::toDto);
    }

    /**
     * Get all the onCallRosters.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<OnCallRosterDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all OnCallRosters");
        return onCallRosterRepository.findAll(pageable).map(onCallRosterMapper::toDto);
    }

    /**
     * Get all the onCallRosters with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<OnCallRosterDTO> findAllWithEagerRelationships(Pageable pageable) {
        return onCallRosterRepository.findAllWithEagerRelationships(pageable).map(onCallRosterMapper::toDto);
    }

    /**
     * Get one onCallRoster by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<OnCallRosterDTO> findOne(UUID id) {
        LOG.debug("Request to get OnCallRoster : {}", id);
        return onCallRosterRepository.findOneWithEagerRelationships(id).map(onCallRosterMapper::toDto);
    }

    /**
     * Delete the onCallRoster by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete OnCallRoster : {}", id);
        onCallRosterRepository.deleteById(id);
    }
}
