package cm.gimac.des.incident.service;

import cm.gimac.des.incident.domain.EngagementFreeze;
import cm.gimac.des.incident.repository.EngagementFreezeRepository;
import cm.gimac.des.incident.service.dto.EngagementFreezeDTO;
import cm.gimac.des.incident.service.mapper.EngagementFreezeMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.incident.domain.EngagementFreeze}.
 */
@Service
@Transactional
public class EngagementFreezeService {

    private static final Logger LOG = LoggerFactory.getLogger(EngagementFreezeService.class);

    private final EngagementFreezeRepository engagementFreezeRepository;

    private final EngagementFreezeMapper engagementFreezeMapper;

    public EngagementFreezeService(EngagementFreezeRepository engagementFreezeRepository, EngagementFreezeMapper engagementFreezeMapper) {
        this.engagementFreezeRepository = engagementFreezeRepository;
        this.engagementFreezeMapper = engagementFreezeMapper;
    }

    /**
     * Save a engagementFreeze.
     *
     * @param engagementFreezeDTO the entity to save.
     * @return the persisted entity.
     */
    public EngagementFreezeDTO save(EngagementFreezeDTO engagementFreezeDTO) {
        LOG.debug("Request to save EngagementFreeze : {}", engagementFreezeDTO);
        EngagementFreeze engagementFreeze = engagementFreezeMapper.toEntity(engagementFreezeDTO);
        engagementFreeze = engagementFreezeRepository.save(engagementFreeze);
        return engagementFreezeMapper.toDto(engagementFreeze);
    }

    /**
     * Update a engagementFreeze.
     *
     * @param engagementFreezeDTO the entity to save.
     * @return the persisted entity.
     */
    public EngagementFreezeDTO update(EngagementFreezeDTO engagementFreezeDTO) {
        LOG.debug("Request to update EngagementFreeze : {}", engagementFreezeDTO);
        EngagementFreeze engagementFreeze = engagementFreezeMapper.toEntity(engagementFreezeDTO);
        engagementFreeze = engagementFreezeRepository.save(engagementFreeze);
        return engagementFreezeMapper.toDto(engagementFreeze);
    }

    /**
     * Partially update a engagementFreeze.
     *
     * @param engagementFreezeDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<EngagementFreezeDTO> partialUpdate(EngagementFreezeDTO engagementFreezeDTO) {
        LOG.debug("Request to partially update EngagementFreeze : {}", engagementFreezeDTO);

        return engagementFreezeRepository
            .findById(engagementFreezeDTO.getId())
            .map(existingEngagementFreeze -> {
                engagementFreezeMapper.partialUpdate(existingEngagementFreeze, engagementFreezeDTO);

                return existingEngagementFreeze;
            })
            .map(engagementFreezeRepository::save)
            .map(engagementFreezeMapper::toDto);
    }

    /**
     * Get all the engagementFreezes.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<EngagementFreezeDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all EngagementFreezes");
        return engagementFreezeRepository.findAll(pageable).map(engagementFreezeMapper::toDto);
    }

    /**
     * Get all the engagementFreezes with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<EngagementFreezeDTO> findAllWithEagerRelationships(Pageable pageable) {
        return engagementFreezeRepository.findAllWithEagerRelationships(pageable).map(engagementFreezeMapper::toDto);
    }

    /**
     * Get one engagementFreeze by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<EngagementFreezeDTO> findOne(UUID id) {
        LOG.debug("Request to get EngagementFreeze : {}", id);
        return engagementFreezeRepository.findOneWithEagerRelationships(id).map(engagementFreezeMapper::toDto);
    }

    /**
     * Delete the engagementFreeze by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete EngagementFreeze : {}", id);
        engagementFreezeRepository.deleteById(id);
    }
}
