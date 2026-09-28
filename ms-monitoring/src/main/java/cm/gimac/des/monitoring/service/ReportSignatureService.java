package cm.gimac.des.monitoring.service;

import cm.gimac.des.monitoring.domain.ReportSignature;
import cm.gimac.des.monitoring.repository.ReportSignatureRepository;
import cm.gimac.des.monitoring.service.dto.ReportSignatureDTO;
import cm.gimac.des.monitoring.service.mapper.ReportSignatureMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.monitoring.domain.ReportSignature}.
 */
@Service
@Transactional
public class ReportSignatureService {

    private static final Logger LOG = LoggerFactory.getLogger(ReportSignatureService.class);

    private final ReportSignatureRepository reportSignatureRepository;

    private final ReportSignatureMapper reportSignatureMapper;

    public ReportSignatureService(ReportSignatureRepository reportSignatureRepository, ReportSignatureMapper reportSignatureMapper) {
        this.reportSignatureRepository = reportSignatureRepository;
        this.reportSignatureMapper = reportSignatureMapper;
    }

    /**
     * Save a reportSignature.
     *
     * @param reportSignatureDTO the entity to save.
     * @return the persisted entity.
     */
    public ReportSignatureDTO save(ReportSignatureDTO reportSignatureDTO) {
        LOG.debug("Request to save ReportSignature : {}", reportSignatureDTO);
        ReportSignature reportSignature = reportSignatureMapper.toEntity(reportSignatureDTO);
        reportSignature = reportSignatureRepository.save(reportSignature);
        return reportSignatureMapper.toDto(reportSignature);
    }

    /**
     * Update a reportSignature.
     *
     * @param reportSignatureDTO the entity to save.
     * @return the persisted entity.
     */
    public ReportSignatureDTO update(ReportSignatureDTO reportSignatureDTO) {
        LOG.debug("Request to update ReportSignature : {}", reportSignatureDTO);
        ReportSignature reportSignature = reportSignatureMapper.toEntity(reportSignatureDTO);
        reportSignature = reportSignatureRepository.save(reportSignature);
        return reportSignatureMapper.toDto(reportSignature);
    }

    /**
     * Partially update a reportSignature.
     *
     * @param reportSignatureDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<ReportSignatureDTO> partialUpdate(ReportSignatureDTO reportSignatureDTO) {
        LOG.debug("Request to partially update ReportSignature : {}", reportSignatureDTO);

        return reportSignatureRepository
            .findById(reportSignatureDTO.getId())
            .map(existingReportSignature -> {
                reportSignatureMapper.partialUpdate(existingReportSignature, reportSignatureDTO);

                return existingReportSignature;
            })
            .map(reportSignatureRepository::save)
            .map(reportSignatureMapper::toDto);
    }

    /**
     * Get all the reportSignatures.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<ReportSignatureDTO> findAll() {
        LOG.debug("Request to get all ReportSignatures");
        return reportSignatureRepository
            .findAll()
            .stream()
            .map(reportSignatureMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all the reportSignatures with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<ReportSignatureDTO> findAllWithEagerRelationships(Pageable pageable) {
        return reportSignatureRepository.findAllWithEagerRelationships(pageable).map(reportSignatureMapper::toDto);
    }

    /**
     * Get one reportSignature by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<ReportSignatureDTO> findOne(UUID id) {
        LOG.debug("Request to get ReportSignature : {}", id);
        return reportSignatureRepository.findOneWithEagerRelationships(id).map(reportSignatureMapper::toDto);
    }

    /**
     * Delete the reportSignature by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete ReportSignature : {}", id);
        reportSignatureRepository.deleteById(id);
    }
}
