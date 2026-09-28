package cm.gimac.des.certification.service;

import cm.gimac.des.certification.domain.ChangeRecord;
import cm.gimac.des.certification.repository.ChangeRecordRepository;
import cm.gimac.des.certification.service.dto.ChangeRecordDTO;
import cm.gimac.des.certification.service.mapper.ChangeRecordMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.certification.domain.ChangeRecord}.
 */
@Service
@Transactional
public class ChangeRecordService {

    private static final Logger LOG = LoggerFactory.getLogger(ChangeRecordService.class);

    private final ChangeRecordRepository changeRecordRepository;

    private final ChangeRecordMapper changeRecordMapper;

    public ChangeRecordService(ChangeRecordRepository changeRecordRepository, ChangeRecordMapper changeRecordMapper) {
        this.changeRecordRepository = changeRecordRepository;
        this.changeRecordMapper = changeRecordMapper;
    }

    /**
     * Save a changeRecord.
     *
     * @param changeRecordDTO the entity to save.
     * @return the persisted entity.
     */
    public ChangeRecordDTO save(ChangeRecordDTO changeRecordDTO) {
        LOG.debug("Request to save ChangeRecord : {}", changeRecordDTO);
        ChangeRecord changeRecord = changeRecordMapper.toEntity(changeRecordDTO);
        changeRecord = changeRecordRepository.save(changeRecord);
        return changeRecordMapper.toDto(changeRecord);
    }

    /**
     * Update a changeRecord.
     *
     * @param changeRecordDTO the entity to save.
     * @return the persisted entity.
     */
    public ChangeRecordDTO update(ChangeRecordDTO changeRecordDTO) {
        LOG.debug("Request to update ChangeRecord : {}", changeRecordDTO);
        ChangeRecord changeRecord = changeRecordMapper.toEntity(changeRecordDTO);
        changeRecord = changeRecordRepository.save(changeRecord);
        return changeRecordMapper.toDto(changeRecord);
    }

    /**
     * Partially update a changeRecord.
     *
     * @param changeRecordDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<ChangeRecordDTO> partialUpdate(ChangeRecordDTO changeRecordDTO) {
        LOG.debug("Request to partially update ChangeRecord : {}", changeRecordDTO);

        return changeRecordRepository
            .findById(changeRecordDTO.getId())
            .map(existingChangeRecord -> {
                changeRecordMapper.partialUpdate(existingChangeRecord, changeRecordDTO);

                return existingChangeRecord;
            })
            .map(changeRecordRepository::save)
            .map(changeRecordMapper::toDto);
    }

    /**
     *  Get all the changeRecords where CommissioningFile is {@code null}.
     *  @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<ChangeRecordDTO> findAllWhereCommissioningFileIsNull() {
        LOG.debug("Request to get all changeRecords where CommissioningFile is null");
        return StreamSupport.stream(changeRecordRepository.findAll().spliterator(), false)
            .filter(changeRecord -> changeRecord.getCommissioningFile() == null)
            .map(changeRecordMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one changeRecord by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<ChangeRecordDTO> findOne(UUID id) {
        LOG.debug("Request to get ChangeRecord : {}", id);
        return changeRecordRepository.findById(id).map(changeRecordMapper::toDto);
    }

    /**
     * Delete the changeRecord by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete ChangeRecord : {}", id);
        changeRecordRepository.deleteById(id);
    }
}
