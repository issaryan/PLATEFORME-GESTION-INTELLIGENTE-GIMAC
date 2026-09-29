package cm.gimac.des.homologation.service.impl;

import cm.gimac.des.homologation.domain.ClosureRecord;
import cm.gimac.des.homologation.repository.ClosureRecordRepository;
import cm.gimac.des.homologation.service.ClosureRecordService;
import cm.gimac.des.homologation.service.dto.ClosureRecordDTO;
import cm.gimac.des.homologation.service.mapper.ClosureRecordMapper;
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
 * Service Implementation for managing {@link cm.gimac.des.homologation.domain.ClosureRecord}.
 */
@Service
@Transactional
public class ClosureRecordServiceImpl implements ClosureRecordService {

    private static final Logger LOG = LoggerFactory.getLogger(ClosureRecordServiceImpl.class);

    private final ClosureRecordRepository closureRecordRepository;

    private final ClosureRecordMapper closureRecordMapper;

    public ClosureRecordServiceImpl(ClosureRecordRepository closureRecordRepository, ClosureRecordMapper closureRecordMapper) {
        this.closureRecordRepository = closureRecordRepository;
        this.closureRecordMapper = closureRecordMapper;
    }

    @Override
    public ClosureRecordDTO save(ClosureRecordDTO closureRecordDTO) {
        LOG.debug("Request to save ClosureRecord : {}", closureRecordDTO);
        ClosureRecord closureRecord = closureRecordMapper.toEntity(closureRecordDTO);
        closureRecord = closureRecordRepository.save(closureRecord);
        return closureRecordMapper.toDto(closureRecord);
    }

    @Override
    public ClosureRecordDTO update(ClosureRecordDTO closureRecordDTO) {
        LOG.debug("Request to update ClosureRecord : {}", closureRecordDTO);
        ClosureRecord closureRecord = closureRecordMapper.toEntity(closureRecordDTO);
        closureRecord = closureRecordRepository.save(closureRecord);
        return closureRecordMapper.toDto(closureRecord);
    }

    @Override
    public Optional<ClosureRecordDTO> partialUpdate(ClosureRecordDTO closureRecordDTO) {
        LOG.debug("Request to partially update ClosureRecord : {}", closureRecordDTO);

        return closureRecordRepository
            .findById(closureRecordDTO.getId())
            .map(existingClosureRecord -> {
                closureRecordMapper.partialUpdate(existingClosureRecord, closureRecordDTO);

                return existingClosureRecord;
            })
            .map(closureRecordRepository::save)
            .map(closureRecordMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClosureRecordDTO> findAll() {
        LOG.debug("Request to get all ClosureRecords");
        return closureRecordRepository.findAll().stream().map(closureRecordMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClosureRecordDTO> findOne(UUID id) {
        LOG.debug("Request to get ClosureRecord : {}", id);
        return closureRecordRepository.findById(id).map(closureRecordMapper::toDto);
    }

    @Override
    public void delete(UUID id) {
        LOG.debug("Request to delete ClosureRecord : {}", id);
        closureRecordRepository.deleteById(id);
    }
}
