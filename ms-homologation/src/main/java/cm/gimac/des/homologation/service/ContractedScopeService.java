package cm.gimac.des.homologation.service;

import cm.gimac.des.homologation.domain.ContractedScope;
import cm.gimac.des.homologation.repository.ContractedScopeRepository;
import cm.gimac.des.homologation.service.dto.ContractedScopeDTO;
import cm.gimac.des.homologation.service.mapper.ContractedScopeMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.homologation.domain.ContractedScope}.
 */
@Service
@Transactional
public class ContractedScopeService {

    private static final Logger LOG = LoggerFactory.getLogger(ContractedScopeService.class);

    private final ContractedScopeRepository contractedScopeRepository;

    private final ContractedScopeMapper contractedScopeMapper;

    public ContractedScopeService(ContractedScopeRepository contractedScopeRepository, ContractedScopeMapper contractedScopeMapper) {
        this.contractedScopeRepository = contractedScopeRepository;
        this.contractedScopeMapper = contractedScopeMapper;
    }

    /**
     * Save a contractedScope.
     *
     * @param contractedScopeDTO the entity to save.
     * @return the persisted entity.
     */
    public ContractedScopeDTO save(ContractedScopeDTO contractedScopeDTO) {
        LOG.debug("Request to save ContractedScope : {}", contractedScopeDTO);
        ContractedScope contractedScope = contractedScopeMapper.toEntity(contractedScopeDTO);
        contractedScope = contractedScopeRepository.save(contractedScope);
        return contractedScopeMapper.toDto(contractedScope);
    }

    /**
     * Update a contractedScope.
     *
     * @param contractedScopeDTO the entity to save.
     * @return the persisted entity.
     */
    public ContractedScopeDTO update(ContractedScopeDTO contractedScopeDTO) {
        LOG.debug("Request to update ContractedScope : {}", contractedScopeDTO);
        ContractedScope contractedScope = contractedScopeMapper.toEntity(contractedScopeDTO);
        contractedScope = contractedScopeRepository.save(contractedScope);
        return contractedScopeMapper.toDto(contractedScope);
    }

    /**
     * Partially update a contractedScope.
     *
     * @param contractedScopeDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<ContractedScopeDTO> partialUpdate(ContractedScopeDTO contractedScopeDTO) {
        LOG.debug("Request to partially update ContractedScope : {}", contractedScopeDTO);

        return contractedScopeRepository
            .findById(contractedScopeDTO.getId())
            .map(existingContractedScope -> {
                contractedScopeMapper.partialUpdate(existingContractedScope, contractedScopeDTO);

                return existingContractedScope;
            })
            .map(contractedScopeRepository::save)
            .map(contractedScopeMapper::toDto);
    }

    /**
     * Get all the contractedScopes with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<ContractedScopeDTO> findAllWithEagerRelationships(Pageable pageable) {
        return contractedScopeRepository.findAllWithEagerRelationships(pageable).map(contractedScopeMapper::toDto);
    }

    /**
     * Get one contractedScope by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<ContractedScopeDTO> findOne(UUID id) {
        LOG.debug("Request to get ContractedScope : {}", id);
        return contractedScopeRepository.findOneWithEagerRelationships(id).map(contractedScopeMapper::toDto);
    }

    /**
     * Delete the contractedScope by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete ContractedScope : {}", id);
        contractedScopeRepository.deleteById(id);
    }
}
