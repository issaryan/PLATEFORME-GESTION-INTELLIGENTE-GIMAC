package cm.gimac.des.pilotage.service;

import cm.gimac.des.pilotage.domain.WipOverride;
import cm.gimac.des.pilotage.repository.WipOverrideRepository;
import cm.gimac.des.pilotage.service.dto.WipOverrideDTO;
import cm.gimac.des.pilotage.service.mapper.WipOverrideMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.pilotage.domain.WipOverride}.
 */
@Service
@Transactional
public class WipOverrideService {

    private static final Logger LOG = LoggerFactory.getLogger(WipOverrideService.class);

    private final WipOverrideRepository wipOverrideRepository;

    private final WipOverrideMapper wipOverrideMapper;

    public WipOverrideService(WipOverrideRepository wipOverrideRepository, WipOverrideMapper wipOverrideMapper) {
        this.wipOverrideRepository = wipOverrideRepository;
        this.wipOverrideMapper = wipOverrideMapper;
    }

    /**
     * Save a wipOverride.
     *
     * @param wipOverrideDTO the entity to save.
     * @return the persisted entity.
     */
    public WipOverrideDTO save(WipOverrideDTO wipOverrideDTO) {
        LOG.debug("Request to save WipOverride : {}", wipOverrideDTO);
        WipOverride wipOverride = wipOverrideMapper.toEntity(wipOverrideDTO);
        wipOverride = wipOverrideRepository.save(wipOverride);
        return wipOverrideMapper.toDto(wipOverride);
    }

    /**
     * Update a wipOverride.
     *
     * @param wipOverrideDTO the entity to save.
     * @return the persisted entity.
     */
    public WipOverrideDTO update(WipOverrideDTO wipOverrideDTO) {
        LOG.debug("Request to update WipOverride : {}", wipOverrideDTO);
        WipOverride wipOverride = wipOverrideMapper.toEntity(wipOverrideDTO);
        wipOverride = wipOverrideRepository.save(wipOverride);
        return wipOverrideMapper.toDto(wipOverride);
    }

    /**
     * Partially update a wipOverride.
     *
     * @param wipOverrideDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<WipOverrideDTO> partialUpdate(WipOverrideDTO wipOverrideDTO) {
        LOG.debug("Request to partially update WipOverride : {}", wipOverrideDTO);

        return wipOverrideRepository
            .findById(wipOverrideDTO.getId())
            .map(existingWipOverride -> {
                wipOverrideMapper.partialUpdate(existingWipOverride, wipOverrideDTO);

                return existingWipOverride;
            })
            .map(wipOverrideRepository::save)
            .map(wipOverrideMapper::toDto);
    }

    /**
     * Get all the wipOverrides with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<WipOverrideDTO> findAllWithEagerRelationships(Pageable pageable) {
        return wipOverrideRepository.findAllWithEagerRelationships(pageable).map(wipOverrideMapper::toDto);
    }

    /**
     * Get one wipOverride by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<WipOverrideDTO> findOne(UUID id) {
        LOG.debug("Request to get WipOverride : {}", id);
        return wipOverrideRepository.findOneWithEagerRelationships(id).map(wipOverrideMapper::toDto);
    }

    /**
     * Delete the wipOverride by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete WipOverride : {}", id);
        wipOverrideRepository.deleteById(id);
    }
}
