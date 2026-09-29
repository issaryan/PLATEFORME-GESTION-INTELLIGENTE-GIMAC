package cm.gimac.des.homologation.service.impl;

import cm.gimac.des.homologation.domain.ReferentialVersion;
import cm.gimac.des.homologation.repository.ReferentialVersionRepository;
import cm.gimac.des.homologation.service.ReferentialVersionService;
import cm.gimac.des.homologation.service.dto.ReferentialVersionDTO;
import cm.gimac.des.homologation.service.mapper.ReferentialVersionMapper;
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
 * Service Implementation for managing {@link cm.gimac.des.homologation.domain.ReferentialVersion}.
 */
@Service
@Transactional
public class ReferentialVersionServiceImpl implements ReferentialVersionService {

    private static final Logger LOG = LoggerFactory.getLogger(ReferentialVersionServiceImpl.class);

    private final ReferentialVersionRepository referentialVersionRepository;

    private final ReferentialVersionMapper referentialVersionMapper;

    public ReferentialVersionServiceImpl(
        ReferentialVersionRepository referentialVersionRepository,
        ReferentialVersionMapper referentialVersionMapper
    ) {
        this.referentialVersionRepository = referentialVersionRepository;
        this.referentialVersionMapper = referentialVersionMapper;
    }

    @Override
    public ReferentialVersionDTO save(ReferentialVersionDTO referentialVersionDTO) {
        LOG.debug("Request to save ReferentialVersion : {}", referentialVersionDTO);
        ReferentialVersion referentialVersion = referentialVersionMapper.toEntity(referentialVersionDTO);
        referentialVersion = referentialVersionRepository.save(referentialVersion);
        return referentialVersionMapper.toDto(referentialVersion);
    }

    @Override
    public ReferentialVersionDTO update(ReferentialVersionDTO referentialVersionDTO) {
        LOG.debug("Request to update ReferentialVersion : {}", referentialVersionDTO);
        ReferentialVersion referentialVersion = referentialVersionMapper.toEntity(referentialVersionDTO);
        referentialVersion = referentialVersionRepository.save(referentialVersion);
        return referentialVersionMapper.toDto(referentialVersion);
    }

    @Override
    public Optional<ReferentialVersionDTO> partialUpdate(ReferentialVersionDTO referentialVersionDTO) {
        LOG.debug("Request to partially update ReferentialVersion : {}", referentialVersionDTO);

        return referentialVersionRepository
            .findById(referentialVersionDTO.getId())
            .map(existingReferentialVersion -> {
                referentialVersionMapper.partialUpdate(existingReferentialVersion, referentialVersionDTO);

                return existingReferentialVersion;
            })
            .map(referentialVersionRepository::save)
            .map(referentialVersionMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReferentialVersionDTO> findAll() {
        LOG.debug("Request to get all ReferentialVersions");
        return referentialVersionRepository
            .findAll()
            .stream()
            .map(referentialVersionMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReferentialVersionDTO> findOne(UUID id) {
        LOG.debug("Request to get ReferentialVersion : {}", id);
        return referentialVersionRepository.findById(id).map(referentialVersionMapper::toDto);
    }

    @Override
    public void delete(UUID id) {
        LOG.debug("Request to delete ReferentialVersion : {}", id);
        referentialVersionRepository.deleteById(id);
    }
}
