package cm.gimac.des.reporting.service;

import cm.gimac.des.reporting.domain.ReportVisa;
import cm.gimac.des.reporting.repository.ReportVisaRepository;
import cm.gimac.des.reporting.service.dto.ReportVisaDTO;
import cm.gimac.des.reporting.service.mapper.ReportVisaMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.reporting.domain.ReportVisa}.
 */
@Service
@Transactional
public class ReportVisaService {

    private static final Logger LOG = LoggerFactory.getLogger(ReportVisaService.class);

    private final ReportVisaRepository reportVisaRepository;

    private final ReportVisaMapper reportVisaMapper;

    public ReportVisaService(ReportVisaRepository reportVisaRepository, ReportVisaMapper reportVisaMapper) {
        this.reportVisaRepository = reportVisaRepository;
        this.reportVisaMapper = reportVisaMapper;
    }

    /**
     * Save a reportVisa.
     *
     * @param reportVisaDTO the entity to save.
     * @return the persisted entity.
     */
    public ReportVisaDTO save(ReportVisaDTO reportVisaDTO) {
        LOG.debug("Request to save ReportVisa : {}", reportVisaDTO);
        ReportVisa reportVisa = reportVisaMapper.toEntity(reportVisaDTO);
        reportVisa = reportVisaRepository.save(reportVisa);
        return reportVisaMapper.toDto(reportVisa);
    }

    /**
     * Update a reportVisa.
     *
     * @param reportVisaDTO the entity to save.
     * @return the persisted entity.
     */
    public ReportVisaDTO update(ReportVisaDTO reportVisaDTO) {
        LOG.debug("Request to update ReportVisa : {}", reportVisaDTO);
        ReportVisa reportVisa = reportVisaMapper.toEntity(reportVisaDTO);
        reportVisa = reportVisaRepository.save(reportVisa);
        return reportVisaMapper.toDto(reportVisa);
    }

    /**
     * Partially update a reportVisa.
     *
     * @param reportVisaDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<ReportVisaDTO> partialUpdate(ReportVisaDTO reportVisaDTO) {
        LOG.debug("Request to partially update ReportVisa : {}", reportVisaDTO);

        return reportVisaRepository
            .findById(reportVisaDTO.getId())
            .map(existingReportVisa -> {
                reportVisaMapper.partialUpdate(existingReportVisa, reportVisaDTO);

                return existingReportVisa;
            })
            .map(reportVisaRepository::save)
            .map(reportVisaMapper::toDto);
    }

    /**
     * Get all the reportVisas.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<ReportVisaDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all ReportVisas");
        return reportVisaRepository.findAll(pageable).map(reportVisaMapper::toDto);
    }

    /**
     * Get one reportVisa by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<ReportVisaDTO> findOne(UUID id) {
        LOG.debug("Request to get ReportVisa : {}", id);
        return reportVisaRepository.findById(id).map(reportVisaMapper::toDto);
    }

    /**
     * Delete the reportVisa by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete ReportVisa : {}", id);
        reportVisaRepository.deleteById(id);
    }
}
