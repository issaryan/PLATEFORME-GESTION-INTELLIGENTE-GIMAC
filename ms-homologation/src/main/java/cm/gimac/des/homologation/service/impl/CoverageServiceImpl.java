package cm.gimac.des.homologation.service.impl;

import cm.gimac.des.homologation.domain.Coverage;
import cm.gimac.des.homologation.repository.CoverageRepository;
import cm.gimac.des.homologation.service.CoverageService;
import cm.gimac.des.homologation.service.dto.CoverageDTO;
import cm.gimac.des.homologation.service.mapper.CoverageMapper;
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
 * Service Implementation for managing {@link cm.gimac.des.homologation.domain.Coverage}.
 */
@Service
@Transactional
public class CoverageServiceImpl implements CoverageService {

    private static final Logger LOG = LoggerFactory.getLogger(CoverageServiceImpl.class);

    private final CoverageRepository coverageRepository;

    private final CoverageMapper coverageMapper;

    public CoverageServiceImpl(CoverageRepository coverageRepository, CoverageMapper coverageMapper) {
        this.coverageRepository = coverageRepository;
        this.coverageMapper = coverageMapper;
    }

    @Override
    public CoverageDTO save(CoverageDTO coverageDTO) {
        LOG.debug("Request to save Coverage : {}", coverageDTO);
        Coverage coverage = coverageMapper.toEntity(coverageDTO);
        coverage = coverageRepository.save(coverage);
        return coverageMapper.toDto(coverage);
    }

    @Override
    public CoverageDTO update(CoverageDTO coverageDTO) {
        LOG.debug("Request to update Coverage : {}", coverageDTO);
        Coverage coverage = coverageMapper.toEntity(coverageDTO);
        coverage = coverageRepository.save(coverage);
        return coverageMapper.toDto(coverage);
    }

    @Override
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

    @Override
    @Transactional(readOnly = true)
    public List<CoverageDTO> findAll() {
        LOG.debug("Request to get all Coverages");
        return coverageRepository.findAll().stream().map(coverageMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CoverageDTO> findOne(UUID id) {
        LOG.debug("Request to get Coverage : {}", id);
        return coverageRepository.findById(id).map(coverageMapper::toDto);
    }

    @Override
    public void delete(UUID id) {
        LOG.debug("Request to delete Coverage : {}", id);
        coverageRepository.deleteById(id);
    }
}
