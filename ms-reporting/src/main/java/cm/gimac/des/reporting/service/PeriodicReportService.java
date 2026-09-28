package cm.gimac.des.reporting.service;

import cm.gimac.des.reporting.domain.PeriodicReport;
import cm.gimac.des.reporting.repository.PeriodicReportRepository;
import cm.gimac.des.reporting.service.dto.PeriodicReportDTO;
import cm.gimac.des.reporting.service.mapper.PeriodicReportMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.reporting.domain.PeriodicReport}.
 */
@Service
@Transactional
public class PeriodicReportService {

    private static final Logger LOG = LoggerFactory.getLogger(PeriodicReportService.class);

    private final PeriodicReportRepository periodicReportRepository;

    private final PeriodicReportMapper periodicReportMapper;

    public PeriodicReportService(PeriodicReportRepository periodicReportRepository, PeriodicReportMapper periodicReportMapper) {
        this.periodicReportRepository = periodicReportRepository;
        this.periodicReportMapper = periodicReportMapper;
    }

    /**
     * Save a periodicReport.
     *
     * @param periodicReportDTO the entity to save.
     * @return the persisted entity.
     */
    public PeriodicReportDTO save(PeriodicReportDTO periodicReportDTO) {
        LOG.debug("Request to save PeriodicReport : {}", periodicReportDTO);
        PeriodicReport periodicReport = periodicReportMapper.toEntity(periodicReportDTO);
        periodicReport = periodicReportRepository.save(periodicReport);
        return periodicReportMapper.toDto(periodicReport);
    }

    /**
     * Update a periodicReport.
     *
     * @param periodicReportDTO the entity to save.
     * @return the persisted entity.
     */
    public PeriodicReportDTO update(PeriodicReportDTO periodicReportDTO) {
        LOG.debug("Request to update PeriodicReport : {}", periodicReportDTO);
        PeriodicReport periodicReport = periodicReportMapper.toEntity(periodicReportDTO);
        periodicReport = periodicReportRepository.save(periodicReport);
        return periodicReportMapper.toDto(periodicReport);
    }

    /**
     * Partially update a periodicReport.
     *
     * @param periodicReportDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<PeriodicReportDTO> partialUpdate(PeriodicReportDTO periodicReportDTO) {
        LOG.debug("Request to partially update PeriodicReport : {}", periodicReportDTO);

        return periodicReportRepository
            .findById(periodicReportDTO.getId())
            .map(existingPeriodicReport -> {
                periodicReportMapper.partialUpdate(existingPeriodicReport, periodicReportDTO);

                return existingPeriodicReport;
            })
            .map(periodicReportRepository::save)
            .map(periodicReportMapper::toDto);
    }

    /**
     * Get one periodicReport by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<PeriodicReportDTO> findOne(UUID id) {
        LOG.debug("Request to get PeriodicReport : {}", id);
        return periodicReportRepository.findById(id).map(periodicReportMapper::toDto);
    }

    /**
     * Delete the periodicReport by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete PeriodicReport : {}", id);
        periodicReportRepository.deleteById(id);
    }
}
