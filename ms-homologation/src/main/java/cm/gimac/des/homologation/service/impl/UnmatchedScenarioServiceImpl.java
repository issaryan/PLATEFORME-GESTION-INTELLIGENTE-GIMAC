package cm.gimac.des.homologation.service.impl;

import cm.gimac.des.homologation.domain.UnmatchedScenario;
import cm.gimac.des.homologation.repository.UnmatchedScenarioRepository;
import cm.gimac.des.homologation.service.UnmatchedScenarioService;
import cm.gimac.des.homologation.service.dto.UnmatchedScenarioDTO;
import cm.gimac.des.homologation.service.mapper.UnmatchedScenarioMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.homologation.domain.UnmatchedScenario}.
 */
@Service
@Transactional
public class UnmatchedScenarioServiceImpl implements UnmatchedScenarioService {

    private static final Logger LOG = LoggerFactory.getLogger(UnmatchedScenarioServiceImpl.class);

    private final UnmatchedScenarioRepository unmatchedScenarioRepository;

    private final UnmatchedScenarioMapper unmatchedScenarioMapper;

    public UnmatchedScenarioServiceImpl(
        UnmatchedScenarioRepository unmatchedScenarioRepository,
        UnmatchedScenarioMapper unmatchedScenarioMapper
    ) {
        this.unmatchedScenarioRepository = unmatchedScenarioRepository;
        this.unmatchedScenarioMapper = unmatchedScenarioMapper;
    }

    @Override
    public UnmatchedScenarioDTO save(UnmatchedScenarioDTO unmatchedScenarioDTO) {
        LOG.debug("Request to save UnmatchedScenario : {}", unmatchedScenarioDTO);
        UnmatchedScenario unmatchedScenario = unmatchedScenarioMapper.toEntity(unmatchedScenarioDTO);
        unmatchedScenario = unmatchedScenarioRepository.save(unmatchedScenario);
        return unmatchedScenarioMapper.toDto(unmatchedScenario);
    }

    @Override
    public UnmatchedScenarioDTO update(UnmatchedScenarioDTO unmatchedScenarioDTO) {
        LOG.debug("Request to update UnmatchedScenario : {}", unmatchedScenarioDTO);
        UnmatchedScenario unmatchedScenario = unmatchedScenarioMapper.toEntity(unmatchedScenarioDTO);
        unmatchedScenario = unmatchedScenarioRepository.save(unmatchedScenario);
        return unmatchedScenarioMapper.toDto(unmatchedScenario);
    }

    @Override
    public Optional<UnmatchedScenarioDTO> partialUpdate(UnmatchedScenarioDTO unmatchedScenarioDTO) {
        LOG.debug("Request to partially update UnmatchedScenario : {}", unmatchedScenarioDTO);

        return unmatchedScenarioRepository
            .findById(unmatchedScenarioDTO.getId())
            .map(existingUnmatchedScenario -> {
                unmatchedScenarioMapper.partialUpdate(existingUnmatchedScenario, unmatchedScenarioDTO);

                return existingUnmatchedScenario;
            })
            .map(unmatchedScenarioRepository::save)
            .map(unmatchedScenarioMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UnmatchedScenarioDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all UnmatchedScenarios");
        return unmatchedScenarioRepository.findAll(pageable).map(unmatchedScenarioMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UnmatchedScenarioDTO> findOne(UUID id) {
        LOG.debug("Request to get UnmatchedScenario : {}", id);
        return unmatchedScenarioRepository.findById(id).map(unmatchedScenarioMapper::toDto);
    }

    @Override
    public void delete(UUID id) {
        LOG.debug("Request to delete UnmatchedScenario : {}", id);
        unmatchedScenarioRepository.deleteById(id);
    }
}
