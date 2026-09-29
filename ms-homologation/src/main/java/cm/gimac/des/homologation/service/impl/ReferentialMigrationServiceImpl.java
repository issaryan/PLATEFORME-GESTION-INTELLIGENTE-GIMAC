package cm.gimac.des.homologation.service.impl;

import cm.gimac.des.homologation.domain.ReferentialMigration;
import cm.gimac.des.homologation.repository.ReferentialMigrationRepository;
import cm.gimac.des.homologation.service.ReferentialMigrationService;
import cm.gimac.des.homologation.service.dto.ReferentialMigrationDTO;
import cm.gimac.des.homologation.service.mapper.ReferentialMigrationMapper;
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
 * Service Implementation for managing {@link cm.gimac.des.homologation.domain.ReferentialMigration}.
 */
@Service
@Transactional
public class ReferentialMigrationServiceImpl implements ReferentialMigrationService {

    private static final Logger LOG = LoggerFactory.getLogger(ReferentialMigrationServiceImpl.class);

    private final ReferentialMigrationRepository referentialMigrationRepository;

    private final ReferentialMigrationMapper referentialMigrationMapper;

    public ReferentialMigrationServiceImpl(
        ReferentialMigrationRepository referentialMigrationRepository,
        ReferentialMigrationMapper referentialMigrationMapper
    ) {
        this.referentialMigrationRepository = referentialMigrationRepository;
        this.referentialMigrationMapper = referentialMigrationMapper;
    }

    @Override
    public ReferentialMigrationDTO save(ReferentialMigrationDTO referentialMigrationDTO) {
        LOG.debug("Request to save ReferentialMigration : {}", referentialMigrationDTO);
        ReferentialMigration referentialMigration = referentialMigrationMapper.toEntity(referentialMigrationDTO);
        referentialMigration = referentialMigrationRepository.save(referentialMigration);
        return referentialMigrationMapper.toDto(referentialMigration);
    }

    @Override
    public ReferentialMigrationDTO update(ReferentialMigrationDTO referentialMigrationDTO) {
        LOG.debug("Request to update ReferentialMigration : {}", referentialMigrationDTO);
        ReferentialMigration referentialMigration = referentialMigrationMapper.toEntity(referentialMigrationDTO);
        referentialMigration = referentialMigrationRepository.save(referentialMigration);
        return referentialMigrationMapper.toDto(referentialMigration);
    }

    @Override
    public Optional<ReferentialMigrationDTO> partialUpdate(ReferentialMigrationDTO referentialMigrationDTO) {
        LOG.debug("Request to partially update ReferentialMigration : {}", referentialMigrationDTO);

        return referentialMigrationRepository
            .findById(referentialMigrationDTO.getId())
            .map(existingReferentialMigration -> {
                referentialMigrationMapper.partialUpdate(existingReferentialMigration, referentialMigrationDTO);

                return existingReferentialMigration;
            })
            .map(referentialMigrationRepository::save)
            .map(referentialMigrationMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReferentialMigrationDTO> findAll() {
        LOG.debug("Request to get all ReferentialMigrations");
        return referentialMigrationRepository
            .findAll()
            .stream()
            .map(referentialMigrationMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReferentialMigrationDTO> findOne(UUID id) {
        LOG.debug("Request to get ReferentialMigration : {}", id);
        return referentialMigrationRepository.findById(id).map(referentialMigrationMapper::toDto);
    }

    @Override
    public void delete(UUID id) {
        LOG.debug("Request to delete ReferentialMigration : {}", id);
        referentialMigrationRepository.deleteById(id);
    }
}
