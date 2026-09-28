package cm.gimac.des.reporting.service;

import cm.gimac.des.reporting.domain.DataExport;
import cm.gimac.des.reporting.repository.DataExportRepository;
import cm.gimac.des.reporting.service.dto.DataExportDTO;
import cm.gimac.des.reporting.service.mapper.DataExportMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.reporting.domain.DataExport}.
 */
@Service
@Transactional
public class DataExportService {

    private static final Logger LOG = LoggerFactory.getLogger(DataExportService.class);

    private final DataExportRepository dataExportRepository;

    private final DataExportMapper dataExportMapper;

    public DataExportService(DataExportRepository dataExportRepository, DataExportMapper dataExportMapper) {
        this.dataExportRepository = dataExportRepository;
        this.dataExportMapper = dataExportMapper;
    }

    /**
     * Save a dataExport.
     *
     * @param dataExportDTO the entity to save.
     * @return the persisted entity.
     */
    public DataExportDTO save(DataExportDTO dataExportDTO) {
        LOG.debug("Request to save DataExport : {}", dataExportDTO);
        DataExport dataExport = dataExportMapper.toEntity(dataExportDTO);
        dataExport = dataExportRepository.save(dataExport);
        return dataExportMapper.toDto(dataExport);
    }

    /**
     * Update a dataExport.
     *
     * @param dataExportDTO the entity to save.
     * @return the persisted entity.
     */
    public DataExportDTO update(DataExportDTO dataExportDTO) {
        LOG.debug("Request to update DataExport : {}", dataExportDTO);
        DataExport dataExport = dataExportMapper.toEntity(dataExportDTO);
        dataExport = dataExportRepository.save(dataExport);
        return dataExportMapper.toDto(dataExport);
    }

    /**
     * Partially update a dataExport.
     *
     * @param dataExportDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<DataExportDTO> partialUpdate(DataExportDTO dataExportDTO) {
        LOG.debug("Request to partially update DataExport : {}", dataExportDTO);

        return dataExportRepository
            .findById(dataExportDTO.getId())
            .map(existingDataExport -> {
                dataExportMapper.partialUpdate(existingDataExport, dataExportDTO);

                return existingDataExport;
            })
            .map(dataExportRepository::save)
            .map(dataExportMapper::toDto);
    }

    /**
     * Get one dataExport by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<DataExportDTO> findOne(UUID id) {
        LOG.debug("Request to get DataExport : {}", id);
        return dataExportRepository.findById(id).map(dataExportMapper::toDto);
    }

    /**
     * Delete the dataExport by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete DataExport : {}", id);
        dataExportRepository.deleteById(id);
    }
}
