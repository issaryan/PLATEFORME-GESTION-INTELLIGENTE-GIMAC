package cm.gimac.des.homologation.service.impl;

import cm.gimac.des.homologation.domain.OperationInstance;
import cm.gimac.des.homologation.repository.OperationInstanceRepository;
import cm.gimac.des.homologation.service.OperationInstanceService;
import cm.gimac.des.homologation.service.dto.OperationInstanceDTO;
import cm.gimac.des.homologation.service.mapper.OperationInstanceMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.homologation.domain.OperationInstance}.
 */
@Service
@Transactional
public class OperationInstanceServiceImpl implements OperationInstanceService {

    private static final Logger LOG = LoggerFactory.getLogger(OperationInstanceServiceImpl.class);

    private final OperationInstanceRepository operationInstanceRepository;

    private final OperationInstanceMapper operationInstanceMapper;

    public OperationInstanceServiceImpl(
        OperationInstanceRepository operationInstanceRepository,
        OperationInstanceMapper operationInstanceMapper
    ) {
        this.operationInstanceRepository = operationInstanceRepository;
        this.operationInstanceMapper = operationInstanceMapper;
    }

    @Override
    public OperationInstanceDTO save(OperationInstanceDTO operationInstanceDTO) {
        LOG.debug("Request to save OperationInstance : {}", operationInstanceDTO);
        OperationInstance operationInstance = operationInstanceMapper.toEntity(operationInstanceDTO);
        operationInstance = operationInstanceRepository.save(operationInstance);
        return operationInstanceMapper.toDto(operationInstance);
    }

    @Override
    public OperationInstanceDTO update(OperationInstanceDTO operationInstanceDTO) {
        LOG.debug("Request to update OperationInstance : {}", operationInstanceDTO);
        OperationInstance operationInstance = operationInstanceMapper.toEntity(operationInstanceDTO);
        operationInstance = operationInstanceRepository.save(operationInstance);
        return operationInstanceMapper.toDto(operationInstance);
    }

    @Override
    public Optional<OperationInstanceDTO> partialUpdate(OperationInstanceDTO operationInstanceDTO) {
        LOG.debug("Request to partially update OperationInstance : {}", operationInstanceDTO);

        return operationInstanceRepository
            .findById(operationInstanceDTO.getId())
            .map(existingOperationInstance -> {
                operationInstanceMapper.partialUpdate(existingOperationInstance, operationInstanceDTO);

                return existingOperationInstance;
            })
            .map(operationInstanceRepository::save)
            .map(operationInstanceMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OperationInstanceDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all OperationInstances");
        return operationInstanceRepository.findAll(pageable).map(operationInstanceMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OperationInstanceDTO> findOne(UUID id) {
        LOG.debug("Request to get OperationInstance : {}", id);
        return operationInstanceRepository.findById(id).map(operationInstanceMapper::toDto);
    }

    @Override
    public void delete(UUID id) {
        LOG.debug("Request to delete OperationInstance : {}", id);
        operationInstanceRepository.deleteById(id);
    }
}
