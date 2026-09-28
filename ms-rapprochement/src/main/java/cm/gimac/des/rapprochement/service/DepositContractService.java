package cm.gimac.des.rapprochement.service;

import cm.gimac.des.rapprochement.domain.DepositContract;
import cm.gimac.des.rapprochement.repository.DepositContractRepository;
import cm.gimac.des.rapprochement.service.dto.DepositContractDTO;
import cm.gimac.des.rapprochement.service.mapper.DepositContractMapper;
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
 * Service Implementation for managing {@link cm.gimac.des.rapprochement.domain.DepositContract}.
 */
@Service
@Transactional
public class DepositContractService {

    private static final Logger LOG = LoggerFactory.getLogger(DepositContractService.class);

    private final DepositContractRepository depositContractRepository;

    private final DepositContractMapper depositContractMapper;

    public DepositContractService(DepositContractRepository depositContractRepository, DepositContractMapper depositContractMapper) {
        this.depositContractRepository = depositContractRepository;
        this.depositContractMapper = depositContractMapper;
    }

    /**
     * Save a depositContract.
     *
     * @param depositContractDTO the entity to save.
     * @return the persisted entity.
     */
    public DepositContractDTO save(DepositContractDTO depositContractDTO) {
        LOG.debug("Request to save DepositContract : {}", depositContractDTO);
        DepositContract depositContract = depositContractMapper.toEntity(depositContractDTO);
        depositContract = depositContractRepository.save(depositContract);
        return depositContractMapper.toDto(depositContract);
    }

    /**
     * Update a depositContract.
     *
     * @param depositContractDTO the entity to save.
     * @return the persisted entity.
     */
    public DepositContractDTO update(DepositContractDTO depositContractDTO) {
        LOG.debug("Request to update DepositContract : {}", depositContractDTO);
        DepositContract depositContract = depositContractMapper.toEntity(depositContractDTO);
        depositContract = depositContractRepository.save(depositContract);
        return depositContractMapper.toDto(depositContract);
    }

    /**
     * Partially update a depositContract.
     *
     * @param depositContractDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<DepositContractDTO> partialUpdate(DepositContractDTO depositContractDTO) {
        LOG.debug("Request to partially update DepositContract : {}", depositContractDTO);

        return depositContractRepository
            .findById(depositContractDTO.getId())
            .map(existingDepositContract -> {
                depositContractMapper.partialUpdate(existingDepositContract, depositContractDTO);

                return existingDepositContract;
            })
            .map(depositContractRepository::save)
            .map(depositContractMapper::toDto);
    }

    /**
     * Get all the depositContracts.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<DepositContractDTO> findAll() {
        LOG.debug("Request to get all DepositContracts");
        return depositContractRepository
            .findAll()
            .stream()
            .map(depositContractMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one depositContract by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<DepositContractDTO> findOne(UUID id) {
        LOG.debug("Request to get DepositContract : {}", id);
        return depositContractRepository.findById(id).map(depositContractMapper::toDto);
    }

    /**
     * Delete the depositContract by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete DepositContract : {}", id);
        depositContractRepository.deleteById(id);
    }
}
