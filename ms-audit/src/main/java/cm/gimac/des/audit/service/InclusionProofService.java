package cm.gimac.des.audit.service;

import cm.gimac.des.audit.domain.InclusionProof;
import cm.gimac.des.audit.repository.InclusionProofRepository;
import cm.gimac.des.audit.service.dto.InclusionProofDTO;
import cm.gimac.des.audit.service.mapper.InclusionProofMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.audit.domain.InclusionProof}.
 */
@Service
@Transactional
public class InclusionProofService {

    private static final Logger LOG = LoggerFactory.getLogger(InclusionProofService.class);

    private final InclusionProofRepository inclusionProofRepository;

    private final InclusionProofMapper inclusionProofMapper;

    public InclusionProofService(InclusionProofRepository inclusionProofRepository, InclusionProofMapper inclusionProofMapper) {
        this.inclusionProofRepository = inclusionProofRepository;
        this.inclusionProofMapper = inclusionProofMapper;
    }

    /**
     * Save a inclusionProof.
     *
     * @param inclusionProofDTO the entity to save.
     * @return the persisted entity.
     */
    public InclusionProofDTO save(InclusionProofDTO inclusionProofDTO) {
        LOG.debug("Request to save InclusionProof : {}", inclusionProofDTO);
        InclusionProof inclusionProof = inclusionProofMapper.toEntity(inclusionProofDTO);
        inclusionProof = inclusionProofRepository.save(inclusionProof);
        return inclusionProofMapper.toDto(inclusionProof);
    }

    /**
     * Update a inclusionProof.
     *
     * @param inclusionProofDTO the entity to save.
     * @return the persisted entity.
     */
    public InclusionProofDTO update(InclusionProofDTO inclusionProofDTO) {
        LOG.debug("Request to update InclusionProof : {}", inclusionProofDTO);
        InclusionProof inclusionProof = inclusionProofMapper.toEntity(inclusionProofDTO);
        inclusionProof = inclusionProofRepository.save(inclusionProof);
        return inclusionProofMapper.toDto(inclusionProof);
    }

    /**
     * Partially update a inclusionProof.
     *
     * @param inclusionProofDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<InclusionProofDTO> partialUpdate(InclusionProofDTO inclusionProofDTO) {
        LOG.debug("Request to partially update InclusionProof : {}", inclusionProofDTO);

        return inclusionProofRepository
            .findById(inclusionProofDTO.getId())
            .map(existingInclusionProof -> {
                inclusionProofMapper.partialUpdate(existingInclusionProof, inclusionProofDTO);

                return existingInclusionProof;
            })
            .map(inclusionProofRepository::save)
            .map(inclusionProofMapper::toDto);
    }

    /**
     * Get all the inclusionProofs.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<InclusionProofDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all InclusionProofs");
        return inclusionProofRepository.findAll(pageable).map(inclusionProofMapper::toDto);
    }

    /**
     * Get all the inclusionProofs with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<InclusionProofDTO> findAllWithEagerRelationships(Pageable pageable) {
        return inclusionProofRepository.findAllWithEagerRelationships(pageable).map(inclusionProofMapper::toDto);
    }

    /**
     * Get one inclusionProof by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<InclusionProofDTO> findOne(UUID id) {
        LOG.debug("Request to get InclusionProof : {}", id);
        return inclusionProofRepository.findOneWithEagerRelationships(id).map(inclusionProofMapper::toDto);
    }

    /**
     * Delete the inclusionProof by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete InclusionProof : {}", id);
        inclusionProofRepository.deleteById(id);
    }
}
