package cm.gimac.des.homologation.service.impl;

import cm.gimac.des.homologation.domain.ReplayCursor;
import cm.gimac.des.homologation.repository.ReplayCursorRepository;
import cm.gimac.des.homologation.service.ReplayCursorService;
import cm.gimac.des.homologation.service.dto.ReplayCursorDTO;
import cm.gimac.des.homologation.service.mapper.ReplayCursorMapper;
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
 * Service Implementation for managing {@link cm.gimac.des.homologation.domain.ReplayCursor}.
 */
@Service
@Transactional
public class ReplayCursorServiceImpl implements ReplayCursorService {

    private static final Logger LOG = LoggerFactory.getLogger(ReplayCursorServiceImpl.class);

    private final ReplayCursorRepository replayCursorRepository;

    private final ReplayCursorMapper replayCursorMapper;

    public ReplayCursorServiceImpl(ReplayCursorRepository replayCursorRepository, ReplayCursorMapper replayCursorMapper) {
        this.replayCursorRepository = replayCursorRepository;
        this.replayCursorMapper = replayCursorMapper;
    }

    @Override
    public ReplayCursorDTO save(ReplayCursorDTO replayCursorDTO) {
        LOG.debug("Request to save ReplayCursor : {}", replayCursorDTO);
        ReplayCursor replayCursor = replayCursorMapper.toEntity(replayCursorDTO);
        replayCursor = replayCursorRepository.save(replayCursor);
        return replayCursorMapper.toDto(replayCursor);
    }

    @Override
    public ReplayCursorDTO update(ReplayCursorDTO replayCursorDTO) {
        LOG.debug("Request to update ReplayCursor : {}", replayCursorDTO);
        ReplayCursor replayCursor = replayCursorMapper.toEntity(replayCursorDTO);
        replayCursor = replayCursorRepository.save(replayCursor);
        return replayCursorMapper.toDto(replayCursor);
    }

    @Override
    public Optional<ReplayCursorDTO> partialUpdate(ReplayCursorDTO replayCursorDTO) {
        LOG.debug("Request to partially update ReplayCursor : {}", replayCursorDTO);

        return replayCursorRepository
            .findById(replayCursorDTO.getId())
            .map(existingReplayCursor -> {
                replayCursorMapper.partialUpdate(existingReplayCursor, replayCursorDTO);

                return existingReplayCursor;
            })
            .map(replayCursorRepository::save)
            .map(replayCursorMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReplayCursorDTO> findAll() {
        LOG.debug("Request to get all ReplayCursors");
        return replayCursorRepository.findAll().stream().map(replayCursorMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReplayCursorDTO> findOne(UUID id) {
        LOG.debug("Request to get ReplayCursor : {}", id);
        return replayCursorRepository.findById(id).map(replayCursorMapper::toDto);
    }

    @Override
    public void delete(UUID id) {
        LOG.debug("Request to delete ReplayCursor : {}", id);
        replayCursorRepository.deleteById(id);
    }
}
