package cm.gimac.des.monitoring.service;

import cm.gimac.des.monitoring.domain.ShiftReport;
import cm.gimac.des.monitoring.repository.ShiftReportRepository;
import cm.gimac.des.monitoring.service.dto.ShiftReportDTO;
import cm.gimac.des.monitoring.service.mapper.ShiftReportMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.monitoring.domain.ShiftReport}.
 */
@Service
@Transactional
public class ShiftReportService {

    private static final Logger LOG = LoggerFactory.getLogger(ShiftReportService.class);

    private final ShiftReportRepository shiftReportRepository;

    private final ShiftReportMapper shiftReportMapper;

    public ShiftReportService(ShiftReportRepository shiftReportRepository, ShiftReportMapper shiftReportMapper) {
        this.shiftReportRepository = shiftReportRepository;
        this.shiftReportMapper = shiftReportMapper;
    }

    /**
     * Save a shiftReport.
     *
     * @param shiftReportDTO the entity to save.
     * @return the persisted entity.
     */
    public ShiftReportDTO save(ShiftReportDTO shiftReportDTO) {
        LOG.debug("Request to save ShiftReport : {}", shiftReportDTO);
        ShiftReport shiftReport = shiftReportMapper.toEntity(shiftReportDTO);
        shiftReport = shiftReportRepository.save(shiftReport);
        return shiftReportMapper.toDto(shiftReport);
    }

    /**
     * Update a shiftReport.
     *
     * @param shiftReportDTO the entity to save.
     * @return the persisted entity.
     */
    public ShiftReportDTO update(ShiftReportDTO shiftReportDTO) {
        LOG.debug("Request to update ShiftReport : {}", shiftReportDTO);
        ShiftReport shiftReport = shiftReportMapper.toEntity(shiftReportDTO);
        shiftReport = shiftReportRepository.save(shiftReport);
        return shiftReportMapper.toDto(shiftReport);
    }

    /**
     * Partially update a shiftReport.
     *
     * @param shiftReportDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<ShiftReportDTO> partialUpdate(ShiftReportDTO shiftReportDTO) {
        LOG.debug("Request to partially update ShiftReport : {}", shiftReportDTO);

        return shiftReportRepository
            .findById(shiftReportDTO.getId())
            .map(existingShiftReport -> {
                shiftReportMapper.partialUpdate(existingShiftReport, shiftReportDTO);

                return existingShiftReport;
            })
            .map(shiftReportRepository::save)
            .map(shiftReportMapper::toDto);
    }

    /**
     * Get all the shiftReports with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<ShiftReportDTO> findAllWithEagerRelationships(Pageable pageable) {
        return shiftReportRepository.findAllWithEagerRelationships(pageable).map(shiftReportMapper::toDto);
    }

    /**
     *  Get all the shiftReports where ShiftReport is {@code null}.
     *  @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<ShiftReportDTO> findAllWhereShiftReportIsNull() {
        LOG.debug("Request to get all shiftReports where ShiftReport is null");
        return StreamSupport.stream(shiftReportRepository.findAll().spliterator(), false)
            .filter(shiftReport -> shiftReport.getShiftReport() == null)
            .map(shiftReportMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one shiftReport by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<ShiftReportDTO> findOne(UUID id) {
        LOG.debug("Request to get ShiftReport : {}", id);
        return shiftReportRepository.findOneWithEagerRelationships(id).map(shiftReportMapper::toDto);
    }

    /**
     * Delete the shiftReport by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete ShiftReport : {}", id);
        shiftReportRepository.deleteById(id);
    }
}
