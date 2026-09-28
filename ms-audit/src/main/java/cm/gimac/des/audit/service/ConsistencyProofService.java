package cm.gimac.des.audit.service;

import cm.gimac.des.audit.domain.ConsistencyProof;
import cm.gimac.des.audit.repository.ConsistencyProofRepository;
import cm.gimac.des.audit.service.dto.ConsistencyProofDTO;
import cm.gimac.des.audit.service.mapper.ConsistencyProofMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.audit.domain.ConsistencyProof}.
 */
@Service
@Transactional
public class ConsistencyProofService {

    private static final Logger LOG = LoggerFactory.getLogger(ConsistencyProofService.class);

    private final ConsistencyProofRepository consistencyProofRepository;

    private final ConsistencyProofMapper consistencyProofMapper;

    public ConsistencyProofService(ConsistencyProofRepository consistencyProofRepository, ConsistencyProofMapper consistencyProofMapper) {
        this.consistencyProofRepository = consistencyProofRepository;
        this.consistencyProofMapper = consistencyProofMapper;
    }

    /**
     * Save a consistencyProof.
     *
     * @param consistencyProofDTO the entity to save.
     * @return the persisted entity.
     */
    public ConsistencyProofDTO save(ConsistencyProofDTO consistencyProofDTO) {
        LOG.debug("Request to save ConsistencyProof : {}", consistencyProofDTO);
        ConsistencyProof consistencyProof = consistencyProofMapper.toEntity(consistencyProofDTO);
        consistencyProof = consistencyProofRepository.save(consistencyProof);
        return consistencyProofMapper.toDto(consistencyProof);
    }

    /**
     * Update a consistencyProof.
     *
     * @param consistencyProofDTO the entity to save.
     * @return the persisted entity.
     */
    public ConsistencyProofDTO update(ConsistencyProofDTO consistencyProofDTO) {
        LOG.debug("Request to update ConsistencyProof : {}", consistencyProofDTO);
        ConsistencyProof consistencyProof = consistencyProofMapper.toEntity(consistencyProofDTO);
        consistencyProof = consistencyProofRepository.save(consistencyProof);
        return consistencyProofMapper.toDto(consistencyProof);
    }

    /**
     * Partially update a consistencyProof.
     *
     * @param consistencyProofDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<ConsistencyProofDTO> partialUpdate(ConsistencyProofDTO consistencyProofDTO) {
        LOG.debug("Request to partially update ConsistencyProof : {}", consistencyProofDTO);

        return consistencyProofRepository
            .findById(consistencyProofDTO.getId())
            .map(existingConsistencyProof -> {
                consistencyProofMapper.partialUpdate(existingConsistencyProof, consistencyProofDTO);

                return existingConsistencyProof;
            })
            .map(consistencyProofRepository::save)
            .map(consistencyProofMapper::toDto);
    }

    /**
     * Get all the consistencyProofs.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<ConsistencyProofDTO> findAll() {
        LOG.debug("Request to get all ConsistencyProofs");
        return consistencyProofRepository
            .findAll()
            .stream()
            .map(consistencyProofMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     *  Get all the consistencyProofs where PeriodRoot is {@code null}.
     *  @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<ConsistencyProofDTO> findAllWherePeriodRootIsNull() {
        LOG.debug("Request to get all consistencyProofs where PeriodRoot is null");
        return StreamSupport.stream(consistencyProofRepository.findAll().spliterator(), false)
            .filter(consistencyProof -> consistencyProof.getPeriodRoot() == null)
            .map(consistencyProofMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one consistencyProof by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<ConsistencyProofDTO> findOne(UUID id) {
        LOG.debug("Request to get ConsistencyProof : {}", id);
        return consistencyProofRepository.findById(id).map(consistencyProofMapper::toDto);
    }

    /**
     * Delete the consistencyProof by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete ConsistencyProof : {}", id);
        consistencyProofRepository.deleteById(id);
    }
}
