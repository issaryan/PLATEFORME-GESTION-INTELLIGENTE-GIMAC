package cm.gimac.des.participant.service;

import cm.gimac.des.participant.domain.Convention;
import cm.gimac.des.participant.repository.ConventionRepository;
import cm.gimac.des.participant.service.dto.ConventionDTO;
import cm.gimac.des.participant.service.mapper.ConventionMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.participant.domain.Convention}.
 */
@Service
@Transactional
public class ConventionService {

    private static final Logger LOG = LoggerFactory.getLogger(ConventionService.class);

    private final ConventionRepository conventionRepository;

    private final ConventionMapper conventionMapper;

    public ConventionService(ConventionRepository conventionRepository, ConventionMapper conventionMapper) {
        this.conventionRepository = conventionRepository;
        this.conventionMapper = conventionMapper;
    }

    /**
     * Save a convention.
     *
     * @param conventionDTO the entity to save.
     * @return the persisted entity.
     */
    public ConventionDTO save(ConventionDTO conventionDTO) {
        LOG.debug("Request to save Convention : {}", conventionDTO);
        Convention convention = conventionMapper.toEntity(conventionDTO);
        convention = conventionRepository.save(convention);
        return conventionMapper.toDto(convention);
    }

    /**
     * Update a convention.
     *
     * @param conventionDTO the entity to save.
     * @return the persisted entity.
     */
    public ConventionDTO update(ConventionDTO conventionDTO) {
        LOG.debug("Request to update Convention : {}", conventionDTO);
        Convention convention = conventionMapper.toEntity(conventionDTO);
        convention = conventionRepository.save(convention);
        return conventionMapper.toDto(convention);
    }

    /**
     * Partially update a convention.
     *
     * @param conventionDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<ConventionDTO> partialUpdate(ConventionDTO conventionDTO) {
        LOG.debug("Request to partially update Convention : {}", conventionDTO);

        return conventionRepository
            .findById(conventionDTO.getId())
            .map(existingConvention -> {
                conventionMapper.partialUpdate(existingConvention, conventionDTO);

                return existingConvention;
            })
            .map(conventionRepository::save)
            .map(conventionMapper::toDto);
    }

    /**
     * Get all the conventions with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<ConventionDTO> findAllWithEagerRelationships(Pageable pageable) {
        return conventionRepository.findAllWithEagerRelationships(pageable).map(conventionMapper::toDto);
    }

    /**
     * Get one convention by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<ConventionDTO> findOne(UUID id) {
        LOG.debug("Request to get Convention : {}", id);
        return conventionRepository.findOneWithEagerRelationships(id).map(conventionMapper::toDto);
    }

    /**
     * Delete the convention by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete Convention : {}", id);
        conventionRepository.deleteById(id);
    }
}
