package cm.gimac.des.iam.service;

import cm.gimac.des.iam.domain.AccessReviewDecision;
import cm.gimac.des.iam.repository.AccessReviewDecisionRepository;
import cm.gimac.des.iam.service.dto.AccessReviewDecisionDTO;
import cm.gimac.des.iam.service.mapper.AccessReviewDecisionMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.iam.domain.AccessReviewDecision}.
 */
@Service
@Transactional
public class AccessReviewDecisionService {

    private static final Logger LOG = LoggerFactory.getLogger(AccessReviewDecisionService.class);

    private final AccessReviewDecisionRepository accessReviewDecisionRepository;

    private final AccessReviewDecisionMapper accessReviewDecisionMapper;

    public AccessReviewDecisionService(
        AccessReviewDecisionRepository accessReviewDecisionRepository,
        AccessReviewDecisionMapper accessReviewDecisionMapper
    ) {
        this.accessReviewDecisionRepository = accessReviewDecisionRepository;
        this.accessReviewDecisionMapper = accessReviewDecisionMapper;
    }

    /**
     * Save a accessReviewDecision.
     *
     * @param accessReviewDecisionDTO the entity to save.
     * @return the persisted entity.
     */
    public AccessReviewDecisionDTO save(AccessReviewDecisionDTO accessReviewDecisionDTO) {
        LOG.debug("Request to save AccessReviewDecision : {}", accessReviewDecisionDTO);
        AccessReviewDecision accessReviewDecision = accessReviewDecisionMapper.toEntity(accessReviewDecisionDTO);
        accessReviewDecision = accessReviewDecisionRepository.save(accessReviewDecision);
        return accessReviewDecisionMapper.toDto(accessReviewDecision);
    }

    /**
     * Update a accessReviewDecision.
     *
     * @param accessReviewDecisionDTO the entity to save.
     * @return the persisted entity.
     */
    public AccessReviewDecisionDTO update(AccessReviewDecisionDTO accessReviewDecisionDTO) {
        LOG.debug("Request to update AccessReviewDecision : {}", accessReviewDecisionDTO);
        AccessReviewDecision accessReviewDecision = accessReviewDecisionMapper.toEntity(accessReviewDecisionDTO);
        accessReviewDecision = accessReviewDecisionRepository.save(accessReviewDecision);
        return accessReviewDecisionMapper.toDto(accessReviewDecision);
    }

    /**
     * Partially update a accessReviewDecision.
     *
     * @param accessReviewDecisionDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<AccessReviewDecisionDTO> partialUpdate(AccessReviewDecisionDTO accessReviewDecisionDTO) {
        LOG.debug("Request to partially update AccessReviewDecision : {}", accessReviewDecisionDTO);

        return accessReviewDecisionRepository
            .findById(accessReviewDecisionDTO.getId())
            .map(existingAccessReviewDecision -> {
                accessReviewDecisionMapper.partialUpdate(existingAccessReviewDecision, accessReviewDecisionDTO);

                return existingAccessReviewDecision;
            })
            .map(accessReviewDecisionRepository::save)
            .map(accessReviewDecisionMapper::toDto);
    }

    /**
     * Get all the accessReviewDecisions.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<AccessReviewDecisionDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all AccessReviewDecisions");
        return accessReviewDecisionRepository.findAll(pageable).map(accessReviewDecisionMapper::toDto);
    }

    /**
     * Get all the accessReviewDecisions with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<AccessReviewDecisionDTO> findAllWithEagerRelationships(Pageable pageable) {
        return accessReviewDecisionRepository.findAllWithEagerRelationships(pageable).map(accessReviewDecisionMapper::toDto);
    }

    /**
     * Get one accessReviewDecision by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<AccessReviewDecisionDTO> findOne(UUID id) {
        LOG.debug("Request to get AccessReviewDecision : {}", id);
        return accessReviewDecisionRepository.findOneWithEagerRelationships(id).map(accessReviewDecisionMapper::toDto);
    }

    /**
     * Delete the accessReviewDecision by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete AccessReviewDecision : {}", id);
        accessReviewDecisionRepository.deleteById(id);
    }
}
