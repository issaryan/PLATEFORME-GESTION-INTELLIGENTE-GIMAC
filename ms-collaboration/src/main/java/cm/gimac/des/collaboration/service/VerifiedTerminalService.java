package cm.gimac.des.collaboration.service;

import cm.gimac.des.collaboration.domain.VerifiedTerminal;
import cm.gimac.des.collaboration.repository.VerifiedTerminalRepository;
import cm.gimac.des.collaboration.service.dto.VerifiedTerminalDTO;
import cm.gimac.des.collaboration.service.mapper.VerifiedTerminalMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.collaboration.domain.VerifiedTerminal}.
 */
@Service
@Transactional
public class VerifiedTerminalService {

    private static final Logger LOG = LoggerFactory.getLogger(VerifiedTerminalService.class);

    private final VerifiedTerminalRepository verifiedTerminalRepository;

    private final VerifiedTerminalMapper verifiedTerminalMapper;

    public VerifiedTerminalService(VerifiedTerminalRepository verifiedTerminalRepository, VerifiedTerminalMapper verifiedTerminalMapper) {
        this.verifiedTerminalRepository = verifiedTerminalRepository;
        this.verifiedTerminalMapper = verifiedTerminalMapper;
    }

    /**
     * Save a verifiedTerminal.
     *
     * @param verifiedTerminalDTO the entity to save.
     * @return the persisted entity.
     */
    public VerifiedTerminalDTO save(VerifiedTerminalDTO verifiedTerminalDTO) {
        LOG.debug("Request to save VerifiedTerminal : {}", verifiedTerminalDTO);
        VerifiedTerminal verifiedTerminal = verifiedTerminalMapper.toEntity(verifiedTerminalDTO);
        verifiedTerminal = verifiedTerminalRepository.save(verifiedTerminal);
        return verifiedTerminalMapper.toDto(verifiedTerminal);
    }

    /**
     * Update a verifiedTerminal.
     *
     * @param verifiedTerminalDTO the entity to save.
     * @return the persisted entity.
     */
    public VerifiedTerminalDTO update(VerifiedTerminalDTO verifiedTerminalDTO) {
        LOG.debug("Request to update VerifiedTerminal : {}", verifiedTerminalDTO);
        VerifiedTerminal verifiedTerminal = verifiedTerminalMapper.toEntity(verifiedTerminalDTO);
        verifiedTerminal = verifiedTerminalRepository.save(verifiedTerminal);
        return verifiedTerminalMapper.toDto(verifiedTerminal);
    }

    /**
     * Partially update a verifiedTerminal.
     *
     * @param verifiedTerminalDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<VerifiedTerminalDTO> partialUpdate(VerifiedTerminalDTO verifiedTerminalDTO) {
        LOG.debug("Request to partially update VerifiedTerminal : {}", verifiedTerminalDTO);

        return verifiedTerminalRepository
            .findById(verifiedTerminalDTO.getId())
            .map(existingVerifiedTerminal -> {
                verifiedTerminalMapper.partialUpdate(existingVerifiedTerminal, verifiedTerminalDTO);

                return existingVerifiedTerminal;
            })
            .map(verifiedTerminalRepository::save)
            .map(verifiedTerminalMapper::toDto);
    }

    /**
     * Get one verifiedTerminal by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<VerifiedTerminalDTO> findOne(UUID id) {
        LOG.debug("Request to get VerifiedTerminal : {}", id);
        return verifiedTerminalRepository.findById(id).map(verifiedTerminalMapper::toDto);
    }

    /**
     * Delete the verifiedTerminal by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete VerifiedTerminal : {}", id);
        verifiedTerminalRepository.deleteById(id);
    }
}
