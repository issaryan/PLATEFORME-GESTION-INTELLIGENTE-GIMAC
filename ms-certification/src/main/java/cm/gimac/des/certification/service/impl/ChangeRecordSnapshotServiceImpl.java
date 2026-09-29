package cm.gimac.des.certification.service.impl;

import cm.gimac.des.certification.domain.ChangeRecordSnapshot;
import cm.gimac.des.certification.repository.ChangeRecordSnapshotRepository;
import cm.gimac.des.certification.service.ChangeRecordSnapshotService;
import cm.gimac.des.certification.service.dto.ChangeRecordSnapshotDTO;
import cm.gimac.des.certification.service.mapper.ChangeRecordSnapshotMapper;
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
 * Service Implementation for managing {@link cm.gimac.des.certification.domain.ChangeRecordSnapshot}.
 */
@Service
@Transactional
public class ChangeRecordSnapshotServiceImpl implements ChangeRecordSnapshotService {

    private static final Logger LOG = LoggerFactory.getLogger(ChangeRecordSnapshotServiceImpl.class);

    private final ChangeRecordSnapshotRepository changeRecordSnapshotRepository;

    private final ChangeRecordSnapshotMapper changeRecordSnapshotMapper;

    public ChangeRecordSnapshotServiceImpl(
        ChangeRecordSnapshotRepository changeRecordSnapshotRepository,
        ChangeRecordSnapshotMapper changeRecordSnapshotMapper
    ) {
        this.changeRecordSnapshotRepository = changeRecordSnapshotRepository;
        this.changeRecordSnapshotMapper = changeRecordSnapshotMapper;
    }

    @Override
    public ChangeRecordSnapshotDTO save(ChangeRecordSnapshotDTO changeRecordSnapshotDTO) {
        LOG.debug("Request to save ChangeRecordSnapshot : {}", changeRecordSnapshotDTO);
        ChangeRecordSnapshot changeRecordSnapshot = changeRecordSnapshotMapper.toEntity(changeRecordSnapshotDTO);
        changeRecordSnapshot = changeRecordSnapshotRepository.save(changeRecordSnapshot);
        return changeRecordSnapshotMapper.toDto(changeRecordSnapshot);
    }

    @Override
    public ChangeRecordSnapshotDTO update(ChangeRecordSnapshotDTO changeRecordSnapshotDTO) {
        LOG.debug("Request to update ChangeRecordSnapshot : {}", changeRecordSnapshotDTO);
        ChangeRecordSnapshot changeRecordSnapshot = changeRecordSnapshotMapper.toEntity(changeRecordSnapshotDTO);
        changeRecordSnapshot = changeRecordSnapshotRepository.save(changeRecordSnapshot);
        return changeRecordSnapshotMapper.toDto(changeRecordSnapshot);
    }

    @Override
    public Optional<ChangeRecordSnapshotDTO> partialUpdate(ChangeRecordSnapshotDTO changeRecordSnapshotDTO) {
        LOG.debug("Request to partially update ChangeRecordSnapshot : {}", changeRecordSnapshotDTO);

        return changeRecordSnapshotRepository
            .findById(changeRecordSnapshotDTO.getId())
            .map(existingChangeRecordSnapshot -> {
                changeRecordSnapshotMapper.partialUpdate(existingChangeRecordSnapshot, changeRecordSnapshotDTO);

                return existingChangeRecordSnapshot;
            })
            .map(changeRecordSnapshotRepository::save)
            .map(changeRecordSnapshotMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChangeRecordSnapshotDTO> findAll() {
        LOG.debug("Request to get all ChangeRecordSnapshots");
        return changeRecordSnapshotRepository
            .findAll()
            .stream()
            .map(changeRecordSnapshotMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ChangeRecordSnapshotDTO> findOne(UUID id) {
        LOG.debug("Request to get ChangeRecordSnapshot : {}", id);
        return changeRecordSnapshotRepository.findById(id).map(changeRecordSnapshotMapper::toDto);
    }

    @Override
    public void delete(UUID id) {
        LOG.debug("Request to delete ChangeRecordSnapshot : {}", id);
        changeRecordSnapshotRepository.deleteById(id);
    }
}
