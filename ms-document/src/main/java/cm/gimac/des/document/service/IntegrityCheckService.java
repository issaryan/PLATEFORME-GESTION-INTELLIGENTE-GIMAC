package cm.gimac.des.document.service;

import cm.gimac.des.document.domain.IntegrityCheck;
import cm.gimac.des.document.repository.IntegrityCheckRepository;
import cm.gimac.des.document.service.dto.IntegrityCheckDTO;
import cm.gimac.des.document.service.mapper.IntegrityCheckMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.document.domain.IntegrityCheck}.
 */
@Service
@Transactional
public class IntegrityCheckService {

    private static final Logger LOG = LoggerFactory.getLogger(IntegrityCheckService.class);

    private final IntegrityCheckRepository integrityCheckRepository;

    private final IntegrityCheckMapper integrityCheckMapper;

    public IntegrityCheckService(IntegrityCheckRepository integrityCheckRepository, IntegrityCheckMapper integrityCheckMapper) {
        this.integrityCheckRepository = integrityCheckRepository;
        this.integrityCheckMapper = integrityCheckMapper;
    }

    /**
     * Save a integrityCheck.
     *
     * @param integrityCheckDTO the entity to save.
     * @return the persisted entity.
     */
    public IntegrityCheckDTO save(IntegrityCheckDTO integrityCheckDTO) {
        LOG.debug("Request to save IntegrityCheck : {}", integrityCheckDTO);
        IntegrityCheck integrityCheck = integrityCheckMapper.toEntity(integrityCheckDTO);
        integrityCheck = integrityCheckRepository.save(integrityCheck);
        return integrityCheckMapper.toDto(integrityCheck);
    }

    /**
     * Update a integrityCheck.
     *
     * @param integrityCheckDTO the entity to save.
     * @return the persisted entity.
     */
    public IntegrityCheckDTO update(IntegrityCheckDTO integrityCheckDTO) {
        LOG.debug("Request to update IntegrityCheck : {}", integrityCheckDTO);
        IntegrityCheck integrityCheck = integrityCheckMapper.toEntity(integrityCheckDTO);
        integrityCheck = integrityCheckRepository.save(integrityCheck);
        return integrityCheckMapper.toDto(integrityCheck);
    }

    /**
     * Partially update a integrityCheck.
     *
     * @param integrityCheckDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<IntegrityCheckDTO> partialUpdate(IntegrityCheckDTO integrityCheckDTO) {
        LOG.debug("Request to partially update IntegrityCheck : {}", integrityCheckDTO);

        return integrityCheckRepository
            .findById(integrityCheckDTO.getId())
            .map(existingIntegrityCheck -> {
                integrityCheckMapper.partialUpdate(existingIntegrityCheck, integrityCheckDTO);

                return existingIntegrityCheck;
            })
            .map(integrityCheckRepository::save)
            .map(integrityCheckMapper::toDto);
    }

    /**
     * Get all the integrityChecks.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<IntegrityCheckDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all IntegrityChecks");
        return integrityCheckRepository.findAll(pageable).map(integrityCheckMapper::toDto);
    }

    /**
     * Get one integrityCheck by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<IntegrityCheckDTO> findOne(UUID id) {
        LOG.debug("Request to get IntegrityCheck : {}", id);
        return integrityCheckRepository.findById(id).map(integrityCheckMapper::toDto);
    }

    /**
     * Delete the integrityCheck by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete IntegrityCheck : {}", id);
        integrityCheckRepository.deleteById(id);
    }
}
