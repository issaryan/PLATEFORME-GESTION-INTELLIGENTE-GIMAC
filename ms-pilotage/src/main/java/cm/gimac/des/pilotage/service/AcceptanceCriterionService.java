package cm.gimac.des.pilotage.service;

import cm.gimac.des.pilotage.domain.AcceptanceCriterion;
import cm.gimac.des.pilotage.repository.AcceptanceCriterionRepository;
import cm.gimac.des.pilotage.service.dto.AcceptanceCriterionDTO;
import cm.gimac.des.pilotage.service.mapper.AcceptanceCriterionMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.pilotage.domain.AcceptanceCriterion}.
 */
@Service
@Transactional
public class AcceptanceCriterionService {

    private static final Logger LOG = LoggerFactory.getLogger(AcceptanceCriterionService.class);

    private final AcceptanceCriterionRepository acceptanceCriterionRepository;

    private final AcceptanceCriterionMapper acceptanceCriterionMapper;

    public AcceptanceCriterionService(
        AcceptanceCriterionRepository acceptanceCriterionRepository,
        AcceptanceCriterionMapper acceptanceCriterionMapper
    ) {
        this.acceptanceCriterionRepository = acceptanceCriterionRepository;
        this.acceptanceCriterionMapper = acceptanceCriterionMapper;
    }

    /**
     * Save a acceptanceCriterion.
     *
     * @param acceptanceCriterionDTO the entity to save.
     * @return the persisted entity.
     */
    public AcceptanceCriterionDTO save(AcceptanceCriterionDTO acceptanceCriterionDTO) {
        LOG.debug("Request to save AcceptanceCriterion : {}", acceptanceCriterionDTO);
        AcceptanceCriterion acceptanceCriterion = acceptanceCriterionMapper.toEntity(acceptanceCriterionDTO);
        acceptanceCriterion = acceptanceCriterionRepository.save(acceptanceCriterion);
        return acceptanceCriterionMapper.toDto(acceptanceCriterion);
    }

    /**
     * Update a acceptanceCriterion.
     *
     * @param acceptanceCriterionDTO the entity to save.
     * @return the persisted entity.
     */
    public AcceptanceCriterionDTO update(AcceptanceCriterionDTO acceptanceCriterionDTO) {
        LOG.debug("Request to update AcceptanceCriterion : {}", acceptanceCriterionDTO);
        AcceptanceCriterion acceptanceCriterion = acceptanceCriterionMapper.toEntity(acceptanceCriterionDTO);
        acceptanceCriterion = acceptanceCriterionRepository.save(acceptanceCriterion);
        return acceptanceCriterionMapper.toDto(acceptanceCriterion);
    }

    /**
     * Partially update a acceptanceCriterion.
     *
     * @param acceptanceCriterionDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<AcceptanceCriterionDTO> partialUpdate(AcceptanceCriterionDTO acceptanceCriterionDTO) {
        LOG.debug("Request to partially update AcceptanceCriterion : {}", acceptanceCriterionDTO);

        return acceptanceCriterionRepository
            .findById(acceptanceCriterionDTO.getId())
            .map(existingAcceptanceCriterion -> {
                acceptanceCriterionMapper.partialUpdate(existingAcceptanceCriterion, acceptanceCriterionDTO);

                return existingAcceptanceCriterion;
            })
            .map(acceptanceCriterionRepository::save)
            .map(acceptanceCriterionMapper::toDto);
    }

    /**
     * Get all the acceptanceCriteria with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<AcceptanceCriterionDTO> findAllWithEagerRelationships(Pageable pageable) {
        return acceptanceCriterionRepository.findAllWithEagerRelationships(pageable).map(acceptanceCriterionMapper::toDto);
    }

    /**
     * Get one acceptanceCriterion by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<AcceptanceCriterionDTO> findOne(UUID id) {
        LOG.debug("Request to get AcceptanceCriterion : {}", id);
        return acceptanceCriterionRepository.findOneWithEagerRelationships(id).map(acceptanceCriterionMapper::toDto);
    }

    /**
     * Delete the acceptanceCriterion by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete AcceptanceCriterion : {}", id);
        acceptanceCriterionRepository.deleteById(id);
    }
}
