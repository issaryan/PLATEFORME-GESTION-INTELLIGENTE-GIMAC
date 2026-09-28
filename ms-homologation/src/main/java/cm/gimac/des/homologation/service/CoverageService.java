package cm.gimac.des.homologation.service;

import cm.gimac.des.homologation.domain.Coverage;
import cm.gimac.des.homologation.repository.CoverageRepository;
import cm.gimac.des.homologation.service.dto.CoverageDTO;
import cm.gimac.des.homologation.service.mapper.CoverageMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.homologation.domain.Coverage}.
 */
@Service
@Transactional
public class CoverageService {

    private static final Logger LOG = LoggerFactory.getLogger(CoverageService.class);

    private final CoverageRepository coverageRepository;

    private final CoverageMapper coverageMapper;

    public CoverageService(CoverageRepository coverageRepository, CoverageMapper coverageMapper) {
        this.coverageRepository = coverageRepository;
        this.coverageMapper = coverageMapper;
    }

    /**
     * Save a coverage.
     *
     * @param coverageDTO the entity to save.
     * @return the persisted entity.
     */
    public CoverageDTO save(CoverageDTO coverageDTO) {
        LOG.debug("Request to save Coverage : {}", coverageDTO);
        Coverage coverage = coverageMapper.toEntity(coverageDTO);
        coverage = coverageRepository.save(coverage);
        return coverageMapper.toDto(coverage);
    }

    /**
     * Update a coverage.
     *
     * @param coverageDTO the entity to save.
     * @return the persisted entity.
     */
    public CoverageDTO update(CoverageDTO coverageDTO) {
        LOG.debug("Request to update Coverage : {}", coverageDTO);
        Coverage coverage = coverageMapper.toEntity(coverageDTO);
        coverage = coverageRepository.save(coverage);
        return coverageMapper.toDto(coverage);
    }

    /**
     * Partially update a coverage.
     *
     * @param coverageDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<CoverageDTO> partialUpdate(CoverageDTO coverageDTO) {
        LOG.debug("Request to partially update Coverage : {}", coverageDTO);

        return coverageRepository
            .findById(coverageDTO.getId())
            .map(existingCoverage -> {
                coverageMapper.partialUpdate(existingCoverage, coverageDTO);

                return existingCoverage;
            })
            .map(coverageRepository::save)
            .map(coverageMapper::toDto);
    }

    /**
     * Get all the coverages with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<CoverageDTO> findAllWithEagerRelationships(Pageable pageable) {
        return coverageRepository.findAllWithEagerRelationships(pageable).map(coverageMapper::toDto);
    }

    /**
     * Get one coverage by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<CoverageDTO> findOne(UUID id) {
        LOG.debug("Request to get Coverage : {}", id);
        return coverageRepository.findOneWithEagerRelationships(id).map(coverageMapper::toDto);
    }

    /**
     * Delete the coverage by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete Coverage : {}", id);
        coverageRepository.deleteById(id);
    }
}
