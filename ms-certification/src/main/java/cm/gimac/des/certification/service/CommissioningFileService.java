package cm.gimac.des.certification.service;

import cm.gimac.des.certification.domain.CommissioningFile;
import cm.gimac.des.certification.repository.CommissioningFileRepository;
import cm.gimac.des.certification.service.dto.CommissioningFileDTO;
import cm.gimac.des.certification.service.mapper.CommissioningFileMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.certification.domain.CommissioningFile}.
 */
@Service
@Transactional
public class CommissioningFileService {

    private static final Logger LOG = LoggerFactory.getLogger(CommissioningFileService.class);

    private final CommissioningFileRepository commissioningFileRepository;

    private final CommissioningFileMapper commissioningFileMapper;

    public CommissioningFileService(
        CommissioningFileRepository commissioningFileRepository,
        CommissioningFileMapper commissioningFileMapper
    ) {
        this.commissioningFileRepository = commissioningFileRepository;
        this.commissioningFileMapper = commissioningFileMapper;
    }

    /**
     * Save a commissioningFile.
     *
     * @param commissioningFileDTO the entity to save.
     * @return the persisted entity.
     */
    public CommissioningFileDTO save(CommissioningFileDTO commissioningFileDTO) {
        LOG.debug("Request to save CommissioningFile : {}", commissioningFileDTO);
        CommissioningFile commissioningFile = commissioningFileMapper.toEntity(commissioningFileDTO);
        commissioningFile = commissioningFileRepository.save(commissioningFile);
        return commissioningFileMapper.toDto(commissioningFile);
    }

    /**
     * Update a commissioningFile.
     *
     * @param commissioningFileDTO the entity to save.
     * @return the persisted entity.
     */
    public CommissioningFileDTO update(CommissioningFileDTO commissioningFileDTO) {
        LOG.debug("Request to update CommissioningFile : {}", commissioningFileDTO);
        CommissioningFile commissioningFile = commissioningFileMapper.toEntity(commissioningFileDTO);
        commissioningFile = commissioningFileRepository.save(commissioningFile);
        return commissioningFileMapper.toDto(commissioningFile);
    }

    /**
     * Partially update a commissioningFile.
     *
     * @param commissioningFileDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<CommissioningFileDTO> partialUpdate(CommissioningFileDTO commissioningFileDTO) {
        LOG.debug("Request to partially update CommissioningFile : {}", commissioningFileDTO);

        return commissioningFileRepository
            .findById(commissioningFileDTO.getId())
            .map(existingCommissioningFile -> {
                commissioningFileMapper.partialUpdate(existingCommissioningFile, commissioningFileDTO);

                return existingCommissioningFile;
            })
            .map(commissioningFileRepository::save)
            .map(commissioningFileMapper::toDto);
    }

    /**
     * Get all the commissioningFiles with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<CommissioningFileDTO> findAllWithEagerRelationships(Pageable pageable) {
        return commissioningFileRepository.findAllWithEagerRelationships(pageable).map(commissioningFileMapper::toDto);
    }

    /**
     * Get one commissioningFile by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<CommissioningFileDTO> findOne(UUID id) {
        LOG.debug("Request to get CommissioningFile : {}", id);
        return commissioningFileRepository.findOneWithEagerRelationships(id).map(commissioningFileMapper::toDto);
    }

    /**
     * Delete the commissioningFile by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete CommissioningFile : {}", id);
        commissioningFileRepository.deleteById(id);
    }
}
