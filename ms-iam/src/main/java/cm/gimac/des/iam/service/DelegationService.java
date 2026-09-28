package cm.gimac.des.iam.service;

import cm.gimac.des.iam.domain.Delegation;
import cm.gimac.des.iam.repository.DelegationRepository;
import cm.gimac.des.iam.service.dto.DelegationDTO;
import cm.gimac.des.iam.service.mapper.DelegationMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.iam.domain.Delegation}.
 */
@Service
@Transactional
public class DelegationService {

    private static final Logger LOG = LoggerFactory.getLogger(DelegationService.class);

    private final DelegationRepository delegationRepository;

    private final DelegationMapper delegationMapper;

    public DelegationService(DelegationRepository delegationRepository, DelegationMapper delegationMapper) {
        this.delegationRepository = delegationRepository;
        this.delegationMapper = delegationMapper;
    }

    /**
     * Save a delegation.
     *
     * @param delegationDTO the entity to save.
     * @return the persisted entity.
     */
    public DelegationDTO save(DelegationDTO delegationDTO) {
        LOG.debug("Request to save Delegation : {}", delegationDTO);
        Delegation delegation = delegationMapper.toEntity(delegationDTO);
        delegation = delegationRepository.save(delegation);
        return delegationMapper.toDto(delegation);
    }

    /**
     * Update a delegation.
     *
     * @param delegationDTO the entity to save.
     * @return the persisted entity.
     */
    public DelegationDTO update(DelegationDTO delegationDTO) {
        LOG.debug("Request to update Delegation : {}", delegationDTO);
        Delegation delegation = delegationMapper.toEntity(delegationDTO);
        delegation = delegationRepository.save(delegation);
        return delegationMapper.toDto(delegation);
    }

    /**
     * Partially update a delegation.
     *
     * @param delegationDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<DelegationDTO> partialUpdate(DelegationDTO delegationDTO) {
        LOG.debug("Request to partially update Delegation : {}", delegationDTO);

        return delegationRepository
            .findById(delegationDTO.getId())
            .map(existingDelegation -> {
                delegationMapper.partialUpdate(existingDelegation, delegationDTO);

                return existingDelegation;
            })
            .map(delegationRepository::save)
            .map(delegationMapper::toDto);
    }

    /**
     * Get all the delegations with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<DelegationDTO> findAllWithEagerRelationships(Pageable pageable) {
        return delegationRepository.findAllWithEagerRelationships(pageable).map(delegationMapper::toDto);
    }

    /**
     * Get one delegation by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<DelegationDTO> findOne(UUID id) {
        LOG.debug("Request to get Delegation : {}", id);
        return delegationRepository.findOneWithEagerRelationships(id).map(delegationMapper::toDto);
    }

    /**
     * Delete the delegation by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete Delegation : {}", id);
        delegationRepository.deleteById(id);
    }
}
