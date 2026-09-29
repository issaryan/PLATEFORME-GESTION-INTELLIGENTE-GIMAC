package cm.gimac.des.certification.service.impl;

import cm.gimac.des.certification.domain.GoLiveReport;
import cm.gimac.des.certification.repository.GoLiveReportRepository;
import cm.gimac.des.certification.service.GoLiveReportService;
import cm.gimac.des.certification.service.dto.GoLiveReportDTO;
import cm.gimac.des.certification.service.mapper.GoLiveReportMapper;
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
 * Service Implementation for managing {@link cm.gimac.des.certification.domain.GoLiveReport}.
 */
@Service
@Transactional
public class GoLiveReportServiceImpl implements GoLiveReportService {

    private static final Logger LOG = LoggerFactory.getLogger(GoLiveReportServiceImpl.class);

    private final GoLiveReportRepository goLiveReportRepository;

    private final GoLiveReportMapper goLiveReportMapper;

    public GoLiveReportServiceImpl(GoLiveReportRepository goLiveReportRepository, GoLiveReportMapper goLiveReportMapper) {
        this.goLiveReportRepository = goLiveReportRepository;
        this.goLiveReportMapper = goLiveReportMapper;
    }

    @Override
    public GoLiveReportDTO save(GoLiveReportDTO goLiveReportDTO) {
        LOG.debug("Request to save GoLiveReport : {}", goLiveReportDTO);
        GoLiveReport goLiveReport = goLiveReportMapper.toEntity(goLiveReportDTO);
        goLiveReport = goLiveReportRepository.save(goLiveReport);
        return goLiveReportMapper.toDto(goLiveReport);
    }

    @Override
    public GoLiveReportDTO update(GoLiveReportDTO goLiveReportDTO) {
        LOG.debug("Request to update GoLiveReport : {}", goLiveReportDTO);
        GoLiveReport goLiveReport = goLiveReportMapper.toEntity(goLiveReportDTO);
        goLiveReport = goLiveReportRepository.save(goLiveReport);
        return goLiveReportMapper.toDto(goLiveReport);
    }

    @Override
    public Optional<GoLiveReportDTO> partialUpdate(GoLiveReportDTO goLiveReportDTO) {
        LOG.debug("Request to partially update GoLiveReport : {}", goLiveReportDTO);

        return goLiveReportRepository
            .findById(goLiveReportDTO.getId())
            .map(existingGoLiveReport -> {
                goLiveReportMapper.partialUpdate(existingGoLiveReport, goLiveReportDTO);

                return existingGoLiveReport;
            })
            .map(goLiveReportRepository::save)
            .map(goLiveReportMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoLiveReportDTO> findAll() {
        LOG.debug("Request to get all GoLiveReports");
        return goLiveReportRepository.findAll().stream().map(goLiveReportMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GoLiveReportDTO> findOne(UUID id) {
        LOG.debug("Request to get GoLiveReport : {}", id);
        return goLiveReportRepository.findById(id).map(goLiveReportMapper::toDto);
    }

    @Override
    public void delete(UUID id) {
        LOG.debug("Request to delete GoLiveReport : {}", id);
        goLiveReportRepository.deleteById(id);
    }
}
