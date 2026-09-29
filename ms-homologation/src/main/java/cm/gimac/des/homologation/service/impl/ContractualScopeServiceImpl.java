package cm.gimac.des.homologation.service.impl;

import cm.gimac.des.homologation.domain.ContractualScope;
import cm.gimac.des.homologation.repository.ContractualScopeRepository;
import cm.gimac.des.homologation.service.ContractualScopeService;
import cm.gimac.des.homologation.service.dto.ContractualScopeDTO;
import cm.gimac.des.homologation.service.mapper.ContractualScopeMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.homologation.domain.ContractualScope}.
 */
@Service
@Transactional
public class ContractualScopeServiceImpl implements ContractualScopeService {

    private static final Logger LOG = LoggerFactory.getLogger(ContractualScopeServiceImpl.class);

    private final ContractualScopeRepository contractualScopeRepository;

    private final ContractualScopeMapper contractualScopeMapper;

    public ContractualScopeServiceImpl(
        ContractualScopeRepository contractualScopeRepository,
        ContractualScopeMapper contractualScopeMapper
    ) {
        this.contractualScopeRepository = contractualScopeRepository;
        this.contractualScopeMapper = contractualScopeMapper;
    }

    @Override
    public ContractualScopeDTO save(ContractualScopeDTO contractualScopeDTO) {
        LOG.debug("Request to save ContractualScope : {}", contractualScopeDTO);
        ContractualScope contractualScope = contractualScopeMapper.toEntity(contractualScopeDTO);
        contractualScope = contractualScopeRepository.save(contractualScope);
        return contractualScopeMapper.toDto(contractualScope);
    }

    @Override
    public ContractualScopeDTO update(ContractualScopeDTO contractualScopeDTO) {
        LOG.debug("Request to update ContractualScope : {}", contractualScopeDTO);
        ContractualScope contractualScope = contractualScopeMapper.toEntity(contractualScopeDTO);
        contractualScope = contractualScopeRepository.save(contractualScope);
        return contractualScopeMapper.toDto(contractualScope);
    }

    @Override
    public Optional<ContractualScopeDTO> partialUpdate(ContractualScopeDTO contractualScopeDTO) {
        LOG.debug("Request to partially update ContractualScope : {}", contractualScopeDTO);

        return contractualScopeRepository
            .findById(contractualScopeDTO.getId())
            .map(existingContractualScope -> {
                contractualScopeMapper.partialUpdate(existingContractualScope, contractualScopeDTO);

                return existingContractualScope;
            })
            .map(contractualScopeRepository::save)
            .map(contractualScopeMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractualScopeDTO> findAll() {
        LOG.debug("Request to get all ContractualScopes");
        return contractualScopeRepository
            .findAll()
            .stream()
            .map(contractualScopeMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ContractualScopeDTO> findOne(UUID id) {
        LOG.debug("Request to get ContractualScope : {}", id);
        return contractualScopeRepository.findById(id).map(contractualScopeMapper::toDto);
    }

    @Override
    public void delete(UUID id) {
        LOG.debug("Request to delete ContractualScope : {}", id);
        contractualScopeRepository.deleteById(id);
    }
}
