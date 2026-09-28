package cm.gimac.des.certification.service;

import cm.gimac.des.certification.domain.CoverageSnapshot;
import cm.gimac.des.certification.repository.CoverageSnapshotRepository;
import cm.gimac.des.certification.service.dto.CoverageSnapshotDTO;
import cm.gimac.des.certification.service.mapper.CoverageSnapshotMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.certification.domain.CoverageSnapshot}.
 */
@Service
@Transactional
public class CoverageSnapshotService {

    private static final Logger LOG = LoggerFactory.getLogger(CoverageSnapshotService.class);

    private final CoverageSnapshotRepository coverageSnapshotRepository;

    private final CoverageSnapshotMapper coverageSnapshotMapper;

    public CoverageSnapshotService(CoverageSnapshotRepository coverageSnapshotRepository, CoverageSnapshotMapper coverageSnapshotMapper) {
        this.coverageSnapshotRepository = coverageSnapshotRepository;
        this.coverageSnapshotMapper = coverageSnapshotMapper;
    }

    /**
     * Save a coverageSnapshot.
     *
     * @param coverageSnapshotDTO the entity to save.
     * @return the persisted entity.
     */
    public CoverageSnapshotDTO save(CoverageSnapshotDTO coverageSnapshotDTO) {
        LOG.debug("Request to save CoverageSnapshot : {}", coverageSnapshotDTO);
        CoverageSnapshot coverageSnapshot = coverageSnapshotMapper.toEntity(coverageSnapshotDTO);
        coverageSnapshot = coverageSnapshotRepository.save(coverageSnapshot);
        return coverageSnapshotMapper.toDto(coverageSnapshot);
    }

    /**
     * Update a coverageSnapshot.
     *
     * @param coverageSnapshotDTO the entity to save.
     * @return the persisted entity.
     */
    public CoverageSnapshotDTO update(CoverageSnapshotDTO coverageSnapshotDTO) {
        LOG.debug("Request to update CoverageSnapshot : {}", coverageSnapshotDTO);
        CoverageSnapshot coverageSnapshot = coverageSnapshotMapper.toEntity(coverageSnapshotDTO);
        coverageSnapshot = coverageSnapshotRepository.save(coverageSnapshot);
        return coverageSnapshotMapper.toDto(coverageSnapshot);
    }

    /**
     * Partially update a coverageSnapshot.
     *
     * @param coverageSnapshotDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<CoverageSnapshotDTO> partialUpdate(CoverageSnapshotDTO coverageSnapshotDTO) {
        LOG.debug("Request to partially update CoverageSnapshot : {}", coverageSnapshotDTO);

        return coverageSnapshotRepository
            .findById(coverageSnapshotDTO.getId())
            .map(existingCoverageSnapshot -> {
                coverageSnapshotMapper.partialUpdate(existingCoverageSnapshot, coverageSnapshotDTO);

                return existingCoverageSnapshot;
            })
            .map(coverageSnapshotRepository::save)
            .map(coverageSnapshotMapper::toDto);
    }

    /**
     * Get one coverageSnapshot by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<CoverageSnapshotDTO> findOne(UUID id) {
        LOG.debug("Request to get CoverageSnapshot : {}", id);
        return coverageSnapshotRepository.findById(id).map(coverageSnapshotMapper::toDto);
    }

    /**
     * Delete the coverageSnapshot by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete CoverageSnapshot : {}", id);
        coverageSnapshotRepository.deleteById(id);
    }
}
