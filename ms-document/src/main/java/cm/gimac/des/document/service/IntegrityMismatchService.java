package cm.gimac.des.document.service;

import cm.gimac.des.document.domain.IntegrityMismatch;
import cm.gimac.des.document.repository.IntegrityMismatchRepository;
import cm.gimac.des.document.service.dto.IntegrityMismatchDTO;
import cm.gimac.des.document.service.mapper.IntegrityMismatchMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.document.domain.IntegrityMismatch}.
 */
@Service
@Transactional
public class IntegrityMismatchService {

    private static final Logger LOG = LoggerFactory.getLogger(IntegrityMismatchService.class);

    private final IntegrityMismatchRepository integrityMismatchRepository;

    private final IntegrityMismatchMapper integrityMismatchMapper;

    public IntegrityMismatchService(
        IntegrityMismatchRepository integrityMismatchRepository,
        IntegrityMismatchMapper integrityMismatchMapper
    ) {
        this.integrityMismatchRepository = integrityMismatchRepository;
        this.integrityMismatchMapper = integrityMismatchMapper;
    }

    /**
     * Save a integrityMismatch.
     *
     * @param integrityMismatchDTO the entity to save.
     * @return the persisted entity.
     */
    public IntegrityMismatchDTO save(IntegrityMismatchDTO integrityMismatchDTO) {
        LOG.debug("Request to save IntegrityMismatch : {}", integrityMismatchDTO);
        IntegrityMismatch integrityMismatch = integrityMismatchMapper.toEntity(integrityMismatchDTO);
        integrityMismatch = integrityMismatchRepository.save(integrityMismatch);
        return integrityMismatchMapper.toDto(integrityMismatch);
    }

    /**
     * Update a integrityMismatch.
     *
     * @param integrityMismatchDTO the entity to save.
     * @return the persisted entity.
     */
    public IntegrityMismatchDTO update(IntegrityMismatchDTO integrityMismatchDTO) {
        LOG.debug("Request to update IntegrityMismatch : {}", integrityMismatchDTO);
        IntegrityMismatch integrityMismatch = integrityMismatchMapper.toEntity(integrityMismatchDTO);
        integrityMismatch = integrityMismatchRepository.save(integrityMismatch);
        return integrityMismatchMapper.toDto(integrityMismatch);
    }

    /**
     * Partially update a integrityMismatch.
     *
     * @param integrityMismatchDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<IntegrityMismatchDTO> partialUpdate(IntegrityMismatchDTO integrityMismatchDTO) {
        LOG.debug("Request to partially update IntegrityMismatch : {}", integrityMismatchDTO);

        return integrityMismatchRepository
            .findById(integrityMismatchDTO.getId())
            .map(existingIntegrityMismatch -> {
                integrityMismatchMapper.partialUpdate(existingIntegrityMismatch, integrityMismatchDTO);

                return existingIntegrityMismatch;
            })
            .map(integrityMismatchRepository::save)
            .map(integrityMismatchMapper::toDto);
    }

    /**
     * Get all the integrityMismatches.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<IntegrityMismatchDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all IntegrityMismatches");
        return integrityMismatchRepository.findAll(pageable).map(integrityMismatchMapper::toDto);
    }

    /**
     * Get all the integrityMismatches with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<IntegrityMismatchDTO> findAllWithEagerRelationships(Pageable pageable) {
        return integrityMismatchRepository.findAllWithEagerRelationships(pageable).map(integrityMismatchMapper::toDto);
    }

    /**
     * Get one integrityMismatch by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<IntegrityMismatchDTO> findOne(UUID id) {
        LOG.debug("Request to get IntegrityMismatch : {}", id);
        return integrityMismatchRepository.findOneWithEagerRelationships(id).map(integrityMismatchMapper::toDto);
    }

    /**
     * Delete the integrityMismatch by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete IntegrityMismatch : {}", id);
        integrityMismatchRepository.deleteById(id);
    }
}
