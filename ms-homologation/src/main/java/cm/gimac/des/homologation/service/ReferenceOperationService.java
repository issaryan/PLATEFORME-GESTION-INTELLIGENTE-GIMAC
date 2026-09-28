package cm.gimac.des.homologation.service;

import cm.gimac.des.homologation.domain.ReferenceOperation;
import cm.gimac.des.homologation.repository.ReferenceOperationRepository;
import cm.gimac.des.homologation.service.dto.ReferenceOperationDTO;
import cm.gimac.des.homologation.service.mapper.ReferenceOperationMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.homologation.domain.ReferenceOperation}.
 */
@Service
@Transactional
public class ReferenceOperationService {

    private static final Logger LOG = LoggerFactory.getLogger(ReferenceOperationService.class);

    private final ReferenceOperationRepository referenceOperationRepository;

    private final ReferenceOperationMapper referenceOperationMapper;

    public ReferenceOperationService(
        ReferenceOperationRepository referenceOperationRepository,
        ReferenceOperationMapper referenceOperationMapper
    ) {
        this.referenceOperationRepository = referenceOperationRepository;
        this.referenceOperationMapper = referenceOperationMapper;
    }

    /**
     * Save a referenceOperation.
     *
     * @param referenceOperationDTO the entity to save.
     * @return the persisted entity.
     */
    public ReferenceOperationDTO save(ReferenceOperationDTO referenceOperationDTO) {
        LOG.debug("Request to save ReferenceOperation : {}", referenceOperationDTO);
        ReferenceOperation referenceOperation = referenceOperationMapper.toEntity(referenceOperationDTO);
        referenceOperation = referenceOperationRepository.save(referenceOperation);
        return referenceOperationMapper.toDto(referenceOperation);
    }

    /**
     * Update a referenceOperation.
     *
     * @param referenceOperationDTO the entity to save.
     * @return the persisted entity.
     */
    public ReferenceOperationDTO update(ReferenceOperationDTO referenceOperationDTO) {
        LOG.debug("Request to update ReferenceOperation : {}", referenceOperationDTO);
        ReferenceOperation referenceOperation = referenceOperationMapper.toEntity(referenceOperationDTO);
        referenceOperation = referenceOperationRepository.save(referenceOperation);
        return referenceOperationMapper.toDto(referenceOperation);
    }

    /**
     * Partially update a referenceOperation.
     *
     * @param referenceOperationDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<ReferenceOperationDTO> partialUpdate(ReferenceOperationDTO referenceOperationDTO) {
        LOG.debug("Request to partially update ReferenceOperation : {}", referenceOperationDTO);

        return referenceOperationRepository
            .findById(referenceOperationDTO.getId())
            .map(existingReferenceOperation -> {
                referenceOperationMapper.partialUpdate(existingReferenceOperation, referenceOperationDTO);

                return existingReferenceOperation;
            })
            .map(referenceOperationRepository::save)
            .map(referenceOperationMapper::toDto);
    }

    /**
     * Get all the referenceOperations with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<ReferenceOperationDTO> findAllWithEagerRelationships(Pageable pageable) {
        return referenceOperationRepository.findAllWithEagerRelationships(pageable).map(referenceOperationMapper::toDto);
    }

    /**
     * Get one referenceOperation by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<ReferenceOperationDTO> findOne(UUID id) {
        LOG.debug("Request to get ReferenceOperation : {}", id);
        return referenceOperationRepository.findOneWithEagerRelationships(id).map(referenceOperationMapper::toDto);
    }

    /**
     * Delete the referenceOperation by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete ReferenceOperation : {}", id);
        referenceOperationRepository.deleteById(id);
    }
}
