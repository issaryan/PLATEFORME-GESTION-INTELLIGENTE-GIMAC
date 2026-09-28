package cm.gimac.des.audit.service;

import cm.gimac.des.audit.domain.VerificationRecord;
import cm.gimac.des.audit.repository.VerificationRecordRepository;
import cm.gimac.des.audit.service.dto.VerificationRecordDTO;
import cm.gimac.des.audit.service.mapper.VerificationRecordMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.audit.domain.VerificationRecord}.
 */
@Service
@Transactional
public class VerificationRecordService {

    private static final Logger LOG = LoggerFactory.getLogger(VerificationRecordService.class);

    private final VerificationRecordRepository verificationRecordRepository;

    private final VerificationRecordMapper verificationRecordMapper;

    public VerificationRecordService(
        VerificationRecordRepository verificationRecordRepository,
        VerificationRecordMapper verificationRecordMapper
    ) {
        this.verificationRecordRepository = verificationRecordRepository;
        this.verificationRecordMapper = verificationRecordMapper;
    }

    /**
     * Save a verificationRecord.
     *
     * @param verificationRecordDTO the entity to save.
     * @return the persisted entity.
     */
    public VerificationRecordDTO save(VerificationRecordDTO verificationRecordDTO) {
        LOG.debug("Request to save VerificationRecord : {}", verificationRecordDTO);
        VerificationRecord verificationRecord = verificationRecordMapper.toEntity(verificationRecordDTO);
        verificationRecord = verificationRecordRepository.save(verificationRecord);
        return verificationRecordMapper.toDto(verificationRecord);
    }

    /**
     * Update a verificationRecord.
     *
     * @param verificationRecordDTO the entity to save.
     * @return the persisted entity.
     */
    public VerificationRecordDTO update(VerificationRecordDTO verificationRecordDTO) {
        LOG.debug("Request to update VerificationRecord : {}", verificationRecordDTO);
        VerificationRecord verificationRecord = verificationRecordMapper.toEntity(verificationRecordDTO);
        verificationRecord = verificationRecordRepository.save(verificationRecord);
        return verificationRecordMapper.toDto(verificationRecord);
    }

    /**
     * Partially update a verificationRecord.
     *
     * @param verificationRecordDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<VerificationRecordDTO> partialUpdate(VerificationRecordDTO verificationRecordDTO) {
        LOG.debug("Request to partially update VerificationRecord : {}", verificationRecordDTO);

        return verificationRecordRepository
            .findById(verificationRecordDTO.getId())
            .map(existingVerificationRecord -> {
                verificationRecordMapper.partialUpdate(existingVerificationRecord, verificationRecordDTO);

                return existingVerificationRecord;
            })
            .map(verificationRecordRepository::save)
            .map(verificationRecordMapper::toDto);
    }

    /**
     * Get one verificationRecord by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<VerificationRecordDTO> findOne(UUID id) {
        LOG.debug("Request to get VerificationRecord : {}", id);
        return verificationRecordRepository.findById(id).map(verificationRecordMapper::toDto);
    }

    /**
     * Delete the verificationRecord by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete VerificationRecord : {}", id);
        verificationRecordRepository.deleteById(id);
    }
}
