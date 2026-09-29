package cm.gimac.des.homologation.service.impl;

import cm.gimac.des.homologation.domain.RejectedRecord;
import cm.gimac.des.homologation.repository.RejectedRecordRepository;
import cm.gimac.des.homologation.service.RejectedRecordService;
import cm.gimac.des.homologation.service.dto.RejectedRecordDTO;
import cm.gimac.des.homologation.service.mapper.RejectedRecordMapper;
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
 * Service Implementation for managing {@link cm.gimac.des.homologation.domain.RejectedRecord}.
 */
@Service
@Transactional
public class RejectedRecordServiceImpl implements RejectedRecordService {

    private static final Logger LOG = LoggerFactory.getLogger(RejectedRecordServiceImpl.class);

    private final RejectedRecordRepository rejectedRecordRepository;

    private final RejectedRecordMapper rejectedRecordMapper;

    public RejectedRecordServiceImpl(RejectedRecordRepository rejectedRecordRepository, RejectedRecordMapper rejectedRecordMapper) {
        this.rejectedRecordRepository = rejectedRecordRepository;
        this.rejectedRecordMapper = rejectedRecordMapper;
    }

    @Override
    public RejectedRecordDTO save(RejectedRecordDTO rejectedRecordDTO) {
        LOG.debug("Request to save RejectedRecord : {}", rejectedRecordDTO);
        RejectedRecord rejectedRecord = rejectedRecordMapper.toEntity(rejectedRecordDTO);
        rejectedRecord = rejectedRecordRepository.save(rejectedRecord);
        return rejectedRecordMapper.toDto(rejectedRecord);
    }

    @Override
    public RejectedRecordDTO update(RejectedRecordDTO rejectedRecordDTO) {
        LOG.debug("Request to update RejectedRecord : {}", rejectedRecordDTO);
        RejectedRecord rejectedRecord = rejectedRecordMapper.toEntity(rejectedRecordDTO);
        rejectedRecord = rejectedRecordRepository.save(rejectedRecord);
        return rejectedRecordMapper.toDto(rejectedRecord);
    }

    @Override
    public Optional<RejectedRecordDTO> partialUpdate(RejectedRecordDTO rejectedRecordDTO) {
        LOG.debug("Request to partially update RejectedRecord : {}", rejectedRecordDTO);

        return rejectedRecordRepository
            .findById(rejectedRecordDTO.getId())
            .map(existingRejectedRecord -> {
                rejectedRecordMapper.partialUpdate(existingRejectedRecord, rejectedRecordDTO);

                return existingRejectedRecord;
            })
            .map(rejectedRecordRepository::save)
            .map(rejectedRecordMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RejectedRecordDTO> findAll() {
        LOG.debug("Request to get all RejectedRecords");
        return rejectedRecordRepository
            .findAll()
            .stream()
            .map(rejectedRecordMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RejectedRecordDTO> findOne(UUID id) {
        LOG.debug("Request to get RejectedRecord : {}", id);
        return rejectedRecordRepository.findById(id).map(rejectedRecordMapper::toDto);
    }

    @Override
    public void delete(UUID id) {
        LOG.debug("Request to delete RejectedRecord : {}", id);
        rejectedRecordRepository.deleteById(id);
    }
}
