package cm.gimac.des.homologation.service.impl;

import cm.gimac.des.homologation.domain.EvidenceFileExport;
import cm.gimac.des.homologation.repository.EvidenceFileExportRepository;
import cm.gimac.des.homologation.service.EvidenceFileExportService;
import cm.gimac.des.homologation.service.dto.EvidenceFileExportDTO;
import cm.gimac.des.homologation.service.mapper.EvidenceFileExportMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.homologation.domain.EvidenceFileExport}.
 */
@Service
@Transactional
public class EvidenceFileExportServiceImpl implements EvidenceFileExportService {

    private static final Logger LOG = LoggerFactory.getLogger(EvidenceFileExportServiceImpl.class);

    private final EvidenceFileExportRepository evidenceFileExportRepository;

    private final EvidenceFileExportMapper evidenceFileExportMapper;

    public EvidenceFileExportServiceImpl(
        EvidenceFileExportRepository evidenceFileExportRepository,
        EvidenceFileExportMapper evidenceFileExportMapper
    ) {
        this.evidenceFileExportRepository = evidenceFileExportRepository;
        this.evidenceFileExportMapper = evidenceFileExportMapper;
    }

    @Override
    public EvidenceFileExportDTO save(EvidenceFileExportDTO evidenceFileExportDTO) {
        LOG.debug("Request to save EvidenceFileExport : {}", evidenceFileExportDTO);
        EvidenceFileExport evidenceFileExport = evidenceFileExportMapper.toEntity(evidenceFileExportDTO);
        evidenceFileExport = evidenceFileExportRepository.save(evidenceFileExport);
        return evidenceFileExportMapper.toDto(evidenceFileExport);
    }

    @Override
    public EvidenceFileExportDTO update(EvidenceFileExportDTO evidenceFileExportDTO) {
        LOG.debug("Request to update EvidenceFileExport : {}", evidenceFileExportDTO);
        EvidenceFileExport evidenceFileExport = evidenceFileExportMapper.toEntity(evidenceFileExportDTO);
        evidenceFileExport = evidenceFileExportRepository.save(evidenceFileExport);
        return evidenceFileExportMapper.toDto(evidenceFileExport);
    }

    @Override
    public Optional<EvidenceFileExportDTO> partialUpdate(EvidenceFileExportDTO evidenceFileExportDTO) {
        LOG.debug("Request to partially update EvidenceFileExport : {}", evidenceFileExportDTO);

        return evidenceFileExportRepository
            .findById(evidenceFileExportDTO.getId())
            .map(existingEvidenceFileExport -> {
                evidenceFileExportMapper.partialUpdate(existingEvidenceFileExport, evidenceFileExportDTO);

                return existingEvidenceFileExport;
            })
            .map(evidenceFileExportRepository::save)
            .map(evidenceFileExportMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvidenceFileExportDTO> findAll() {
        LOG.debug("Request to get all EvidenceFileExports");
        return evidenceFileExportRepository
            .findAll()
            .stream()
            .map(evidenceFileExportMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EvidenceFileExportDTO> findOne(UUID id) {
        LOG.debug("Request to get EvidenceFileExport : {}", id);
        return evidenceFileExportRepository.findById(id).map(evidenceFileExportMapper::toDto);
    }

    @Override
    public void delete(UUID id) {
        LOG.debug("Request to delete EvidenceFileExport : {}", id);
        evidenceFileExportRepository.deleteById(id);
    }
}
