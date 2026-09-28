package cm.gimac.des.homologation.service;

import cm.gimac.des.homologation.domain.Operation;
import cm.gimac.des.homologation.repository.OperationRepository;
import cm.gimac.des.homologation.service.dto.OperationDTO;
import cm.gimac.des.homologation.service.mapper.OperationMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.homologation.domain.Operation}.
 */
@Service
@Transactional
public class OperationService {

    private static final Logger LOG = LoggerFactory.getLogger(OperationService.class);

    private final OperationRepository operationRepository;

    private final OperationMapper operationMapper;

    public OperationService(OperationRepository operationRepository, OperationMapper operationMapper) {
        this.operationRepository = operationRepository;
        this.operationMapper = operationMapper;
    }

    /**
     * Save a operation.
     *
     * @param operationDTO the entity to save.
     * @return the persisted entity.
     */
    public OperationDTO save(OperationDTO operationDTO) {
        LOG.debug("Request to save Operation : {}", operationDTO);
        Operation operation = operationMapper.toEntity(operationDTO);
        operation = operationRepository.save(operation);
        return operationMapper.toDto(operation);
    }

    /**
     * Update a operation.
     *
     * @param operationDTO the entity to save.
     * @return the persisted entity.
     */
    public OperationDTO update(OperationDTO operationDTO) {
        LOG.debug("Request to update Operation : {}", operationDTO);
        Operation operation = operationMapper.toEntity(operationDTO);
        operation = operationRepository.save(operation);
        return operationMapper.toDto(operation);
    }

    /**
     * Partially update a operation.
     *
     * @param operationDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<OperationDTO> partialUpdate(OperationDTO operationDTO) {
        LOG.debug("Request to partially update Operation : {}", operationDTO);

        return operationRepository
            .findById(operationDTO.getId())
            .map(existingOperation -> {
                operationMapper.partialUpdate(existingOperation, operationDTO);

                return existingOperation;
            })
            .map(operationRepository::save)
            .map(operationMapper::toDto);
    }

    /**
     * Get all the operations with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<OperationDTO> findAllWithEagerRelationships(Pageable pageable) {
        return operationRepository.findAllWithEagerRelationships(pageable).map(operationMapper::toDto);
    }

    /**
     * Get one operation by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<OperationDTO> findOne(UUID id) {
        LOG.debug("Request to get Operation : {}", id);
        return operationRepository.findOneWithEagerRelationships(id).map(operationMapper::toDto);
    }

    /**
     * Delete the operation by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete Operation : {}", id);
        operationRepository.deleteById(id);
    }
}
