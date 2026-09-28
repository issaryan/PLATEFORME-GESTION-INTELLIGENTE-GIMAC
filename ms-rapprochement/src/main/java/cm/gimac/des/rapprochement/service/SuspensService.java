package cm.gimac.des.rapprochement.service;

import cm.gimac.des.rapprochement.domain.Suspens;
import cm.gimac.des.rapprochement.repository.SuspensRepository;
import cm.gimac.des.rapprochement.service.dto.SuspensDTO;
import cm.gimac.des.rapprochement.service.mapper.SuspensMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.rapprochement.domain.Suspens}.
 */
@Service
@Transactional
public class SuspensService {

    private static final Logger LOG = LoggerFactory.getLogger(SuspensService.class);

    private final SuspensRepository suspensRepository;

    private final SuspensMapper suspensMapper;

    public SuspensService(SuspensRepository suspensRepository, SuspensMapper suspensMapper) {
        this.suspensRepository = suspensRepository;
        this.suspensMapper = suspensMapper;
    }

    /**
     * Save a suspens.
     *
     * @param suspensDTO the entity to save.
     * @return the persisted entity.
     */
    public SuspensDTO save(SuspensDTO suspensDTO) {
        LOG.debug("Request to save Suspens : {}", suspensDTO);
        Suspens suspens = suspensMapper.toEntity(suspensDTO);
        suspens = suspensRepository.save(suspens);
        return suspensMapper.toDto(suspens);
    }

    /**
     * Update a suspens.
     *
     * @param suspensDTO the entity to save.
     * @return the persisted entity.
     */
    public SuspensDTO update(SuspensDTO suspensDTO) {
        LOG.debug("Request to update Suspens : {}", suspensDTO);
        Suspens suspens = suspensMapper.toEntity(suspensDTO);
        suspens = suspensRepository.save(suspens);
        return suspensMapper.toDto(suspens);
    }

    /**
     * Partially update a suspens.
     *
     * @param suspensDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<SuspensDTO> partialUpdate(SuspensDTO suspensDTO) {
        LOG.debug("Request to partially update Suspens : {}", suspensDTO);

        return suspensRepository
            .findById(suspensDTO.getId())
            .map(existingSuspens -> {
                suspensMapper.partialUpdate(existingSuspens, suspensDTO);

                return existingSuspens;
            })
            .map(suspensRepository::save)
            .map(suspensMapper::toDto);
    }

    /**
     * Get all the suspens with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<SuspensDTO> findAllWithEagerRelationships(Pageable pageable) {
        return suspensRepository.findAllWithEagerRelationships(pageable).map(suspensMapper::toDto);
    }

    /**
     *  Get all the suspens where RegularizationAttestation is {@code null}.
     *  @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<SuspensDTO> findAllWhereRegularizationAttestationIsNull() {
        LOG.debug("Request to get all suspens where RegularizationAttestation is null");
        return StreamSupport.stream(suspensRepository.findAll().spliterator(), false)
            .filter(suspens -> suspens.getRegularizationAttestation() == null)
            .map(suspensMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one suspens by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<SuspensDTO> findOne(UUID id) {
        LOG.debug("Request to get Suspens : {}", id);
        return suspensRepository.findOneWithEagerRelationships(id).map(suspensMapper::toDto);
    }

    /**
     * Delete the suspens by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete Suspens : {}", id);
        suspensRepository.deleteById(id);
    }
}
