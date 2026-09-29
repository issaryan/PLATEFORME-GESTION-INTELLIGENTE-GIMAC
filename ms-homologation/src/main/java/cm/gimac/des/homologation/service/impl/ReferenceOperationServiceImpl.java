package cm.gimac.des.homologation.service.impl;

import cm.gimac.des.homologation.domain.ReferenceOperation;
import cm.gimac.des.homologation.repository.ReferenceOperationRepository;
import cm.gimac.des.homologation.service.ReferenceOperationService;
import cm.gimac.des.homologation.service.dto.ReferenceOperationDTO;
import cm.gimac.des.homologation.service.mapper.ReferenceOperationMapper;
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
 * Service Implementation for managing {@link cm.gimac.des.homologation.domain.ReferenceOperation}.
 */
@Service
@Transactional
public class ReferenceOperationServiceImpl implements ReferenceOperationService {

    private static final Logger LOG = LoggerFactory.getLogger(ReferenceOperationServiceImpl.class);

    private final ReferenceOperationRepository referenceOperationRepository;

    private final ReferenceOperationMapper referenceOperationMapper;

    public ReferenceOperationServiceImpl(
        ReferenceOperationRepository referenceOperationRepository,
        ReferenceOperationMapper referenceOperationMapper
    ) {
        this.referenceOperationRepository = referenceOperationRepository;
        this.referenceOperationMapper = referenceOperationMapper;
    }

    @Override
    public ReferenceOperationDTO save(ReferenceOperationDTO referenceOperationDTO) {
        LOG.debug("Request to save ReferenceOperation : {}", referenceOperationDTO);
        ReferenceOperation referenceOperation = referenceOperationMapper.toEntity(referenceOperationDTO);
        referenceOperation = referenceOperationRepository.save(referenceOperation);
        return referenceOperationMapper.toDto(referenceOperation);
    }

    @Override
    public ReferenceOperationDTO update(ReferenceOperationDTO referenceOperationDTO) {
        LOG.debug("Request to update ReferenceOperation : {}", referenceOperationDTO);
        ReferenceOperation referenceOperation = referenceOperationMapper.toEntity(referenceOperationDTO);
        referenceOperation = referenceOperationRepository.save(referenceOperation);
        return referenceOperationMapper.toDto(referenceOperation);
    }

    @Override
    public Optional<ReferenceOperationDTO> partialUpdate(ReferenceOperationDTO referenceOperationDTO) {
        LOG.debug("Request to partially update ReferenceOperation : {}", referenceOperationDTO);

        return referenceOperationRepository
            .findById(referenceOperationDTO.getId())
            .map(existingReferenceOperation -> {
                referenceOperationMapper.partialUpdate(existingReferenceOperation, referenceOperationDTO);

                return existingReferenceOperation;
            })
            .map(referenceOperationRepository::save)
            .map(referenceOperationMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReferenceOperationDTO> findAll() {
        LOG.debug("Request to get all ReferenceOperations");
        return referenceOperationRepository
            .findAll()
            .stream()
            .map(referenceOperationMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReferenceOperationDTO> findOne(UUID id) {
        LOG.debug("Request to get ReferenceOperation : {}", id);
        return referenceOperationRepository.findById(id).map(referenceOperationMapper::toDto);
    }

    @Override
    public void delete(UUID id) {
        LOG.debug("Request to delete ReferenceOperation : {}", id);
        referenceOperationRepository.deleteById(id);
    }
}
