package cm.gimac.des.notification.service;

import cm.gimac.des.notification.domain.ConsolidationWindow;
import cm.gimac.des.notification.repository.ConsolidationWindowRepository;
import cm.gimac.des.notification.service.dto.ConsolidationWindowDTO;
import cm.gimac.des.notification.service.mapper.ConsolidationWindowMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.notification.domain.ConsolidationWindow}.
 */
@Service
@Transactional
public class ConsolidationWindowService {

    private static final Logger LOG = LoggerFactory.getLogger(ConsolidationWindowService.class);

    private final ConsolidationWindowRepository consolidationWindowRepository;

    private final ConsolidationWindowMapper consolidationWindowMapper;

    public ConsolidationWindowService(
        ConsolidationWindowRepository consolidationWindowRepository,
        ConsolidationWindowMapper consolidationWindowMapper
    ) {
        this.consolidationWindowRepository = consolidationWindowRepository;
        this.consolidationWindowMapper = consolidationWindowMapper;
    }

    /**
     * Save a consolidationWindow.
     *
     * @param consolidationWindowDTO the entity to save.
     * @return the persisted entity.
     */
    public ConsolidationWindowDTO save(ConsolidationWindowDTO consolidationWindowDTO) {
        LOG.debug("Request to save ConsolidationWindow : {}", consolidationWindowDTO);
        ConsolidationWindow consolidationWindow = consolidationWindowMapper.toEntity(consolidationWindowDTO);
        consolidationWindow = consolidationWindowRepository.save(consolidationWindow);
        return consolidationWindowMapper.toDto(consolidationWindow);
    }

    /**
     * Update a consolidationWindow.
     *
     * @param consolidationWindowDTO the entity to save.
     * @return the persisted entity.
     */
    public ConsolidationWindowDTO update(ConsolidationWindowDTO consolidationWindowDTO) {
        LOG.debug("Request to update ConsolidationWindow : {}", consolidationWindowDTO);
        ConsolidationWindow consolidationWindow = consolidationWindowMapper.toEntity(consolidationWindowDTO);
        consolidationWindow = consolidationWindowRepository.save(consolidationWindow);
        return consolidationWindowMapper.toDto(consolidationWindow);
    }

    /**
     * Partially update a consolidationWindow.
     *
     * @param consolidationWindowDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<ConsolidationWindowDTO> partialUpdate(ConsolidationWindowDTO consolidationWindowDTO) {
        LOG.debug("Request to partially update ConsolidationWindow : {}", consolidationWindowDTO);

        return consolidationWindowRepository
            .findById(consolidationWindowDTO.getId())
            .map(existingConsolidationWindow -> {
                consolidationWindowMapper.partialUpdate(existingConsolidationWindow, consolidationWindowDTO);

                return existingConsolidationWindow;
            })
            .map(consolidationWindowRepository::save)
            .map(consolidationWindowMapper::toDto);
    }

    /**
     * Get all the consolidationWindows.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<ConsolidationWindowDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all ConsolidationWindows");
        return consolidationWindowRepository.findAll(pageable).map(consolidationWindowMapper::toDto);
    }

    /**
     * Get one consolidationWindow by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<ConsolidationWindowDTO> findOne(UUID id) {
        LOG.debug("Request to get ConsolidationWindow : {}", id);
        return consolidationWindowRepository.findById(id).map(consolidationWindowMapper::toDto);
    }

    /**
     * Delete the consolidationWindow by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete ConsolidationWindow : {}", id);
        consolidationWindowRepository.deleteById(id);
    }
}
