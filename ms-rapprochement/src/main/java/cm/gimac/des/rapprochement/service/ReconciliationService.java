package cm.gimac.des.rapprochement.service;

import cm.gimac.des.rapprochement.domain.Reconciliation;
import cm.gimac.des.rapprochement.repository.ReconciliationRepository;
import cm.gimac.des.rapprochement.service.dto.ReconciliationDTO;
import cm.gimac.des.rapprochement.service.mapper.ReconciliationMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.rapprochement.domain.Reconciliation}.
 */
@Service
@Transactional
public class ReconciliationService {

    private static final Logger LOG = LoggerFactory.getLogger(ReconciliationService.class);

    private final ReconciliationRepository reconciliationRepository;

    private final ReconciliationMapper reconciliationMapper;

    public ReconciliationService(ReconciliationRepository reconciliationRepository, ReconciliationMapper reconciliationMapper) {
        this.reconciliationRepository = reconciliationRepository;
        this.reconciliationMapper = reconciliationMapper;
    }

    /**
     * Save a reconciliation.
     *
     * @param reconciliationDTO the entity to save.
     * @return the persisted entity.
     */
    public ReconciliationDTO save(ReconciliationDTO reconciliationDTO) {
        LOG.debug("Request to save Reconciliation : {}", reconciliationDTO);
        Reconciliation reconciliation = reconciliationMapper.toEntity(reconciliationDTO);
        reconciliation = reconciliationRepository.save(reconciliation);
        return reconciliationMapper.toDto(reconciliation);
    }

    /**
     * Update a reconciliation.
     *
     * @param reconciliationDTO the entity to save.
     * @return the persisted entity.
     */
    public ReconciliationDTO update(ReconciliationDTO reconciliationDTO) {
        LOG.debug("Request to update Reconciliation : {}", reconciliationDTO);
        Reconciliation reconciliation = reconciliationMapper.toEntity(reconciliationDTO);
        reconciliation = reconciliationRepository.save(reconciliation);
        return reconciliationMapper.toDto(reconciliation);
    }

    /**
     * Partially update a reconciliation.
     *
     * @param reconciliationDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<ReconciliationDTO> partialUpdate(ReconciliationDTO reconciliationDTO) {
        LOG.debug("Request to partially update Reconciliation : {}", reconciliationDTO);

        return reconciliationRepository
            .findById(reconciliationDTO.getId())
            .map(existingReconciliation -> {
                reconciliationMapper.partialUpdate(existingReconciliation, reconciliationDTO);

                return existingReconciliation;
            })
            .map(reconciliationRepository::save)
            .map(reconciliationMapper::toDto);
    }

    /**
     * Get all the reconciliations with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<ReconciliationDTO> findAllWithEagerRelationships(Pageable pageable) {
        return reconciliationRepository.findAllWithEagerRelationships(pageable).map(reconciliationMapper::toDto);
    }

    /**
     * Get one reconciliation by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<ReconciliationDTO> findOne(UUID id) {
        LOG.debug("Request to get Reconciliation : {}", id);
        return reconciliationRepository.findOneWithEagerRelationships(id).map(reconciliationMapper::toDto);
    }

    /**
     * Delete the reconciliation by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete Reconciliation : {}", id);
        reconciliationRepository.deleteById(id);
    }
}
