package cm.gimac.des.pilotage.service;

import cm.gimac.des.pilotage.domain.ActionSheet;
import cm.gimac.des.pilotage.repository.ActionSheetRepository;
import cm.gimac.des.pilotage.service.dto.ActionSheetDTO;
import cm.gimac.des.pilotage.service.mapper.ActionSheetMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.pilotage.domain.ActionSheet}.
 */
@Service
@Transactional
public class ActionSheetService {

    private static final Logger LOG = LoggerFactory.getLogger(ActionSheetService.class);

    private final ActionSheetRepository actionSheetRepository;

    private final ActionSheetMapper actionSheetMapper;

    public ActionSheetService(ActionSheetRepository actionSheetRepository, ActionSheetMapper actionSheetMapper) {
        this.actionSheetRepository = actionSheetRepository;
        this.actionSheetMapper = actionSheetMapper;
    }

    /**
     * Save a actionSheet.
     *
     * @param actionSheetDTO the entity to save.
     * @return the persisted entity.
     */
    public ActionSheetDTO save(ActionSheetDTO actionSheetDTO) {
        LOG.debug("Request to save ActionSheet : {}", actionSheetDTO);
        ActionSheet actionSheet = actionSheetMapper.toEntity(actionSheetDTO);
        actionSheet = actionSheetRepository.save(actionSheet);
        return actionSheetMapper.toDto(actionSheet);
    }

    /**
     * Update a actionSheet.
     *
     * @param actionSheetDTO the entity to save.
     * @return the persisted entity.
     */
    public ActionSheetDTO update(ActionSheetDTO actionSheetDTO) {
        LOG.debug("Request to update ActionSheet : {}", actionSheetDTO);
        ActionSheet actionSheet = actionSheetMapper.toEntity(actionSheetDTO);
        actionSheet = actionSheetRepository.save(actionSheet);
        return actionSheetMapper.toDto(actionSheet);
    }

    /**
     * Partially update a actionSheet.
     *
     * @param actionSheetDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<ActionSheetDTO> partialUpdate(ActionSheetDTO actionSheetDTO) {
        LOG.debug("Request to partially update ActionSheet : {}", actionSheetDTO);

        return actionSheetRepository
            .findById(actionSheetDTO.getId())
            .map(existingActionSheet -> {
                actionSheetMapper.partialUpdate(existingActionSheet, actionSheetDTO);

                return existingActionSheet;
            })
            .map(actionSheetRepository::save)
            .map(actionSheetMapper::toDto);
    }

    /**
     * Get all the actionSheets with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<ActionSheetDTO> findAllWithEagerRelationships(Pageable pageable) {
        return actionSheetRepository.findAllWithEagerRelationships(pageable).map(actionSheetMapper::toDto);
    }

    /**
     * Get one actionSheet by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<ActionSheetDTO> findOne(UUID id) {
        LOG.debug("Request to get ActionSheet : {}", id);
        return actionSheetRepository.findOneWithEagerRelationships(id).map(actionSheetMapper::toDto);
    }

    /**
     * Delete the actionSheet by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete ActionSheet : {}", id);
        actionSheetRepository.deleteById(id);
    }
}
