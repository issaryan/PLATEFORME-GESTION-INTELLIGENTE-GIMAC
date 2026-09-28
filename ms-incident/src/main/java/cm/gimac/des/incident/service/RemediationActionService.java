package cm.gimac.des.incident.service;

import cm.gimac.des.incident.domain.RemediationAction;
import cm.gimac.des.incident.repository.RemediationActionRepository;
import cm.gimac.des.incident.service.dto.RemediationActionDTO;
import cm.gimac.des.incident.service.mapper.RemediationActionMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.incident.domain.RemediationAction}.
 */
@Service
@Transactional
public class RemediationActionService {

    private static final Logger LOG = LoggerFactory.getLogger(RemediationActionService.class);

    private final RemediationActionRepository remediationActionRepository;

    private final RemediationActionMapper remediationActionMapper;

    public RemediationActionService(
        RemediationActionRepository remediationActionRepository,
        RemediationActionMapper remediationActionMapper
    ) {
        this.remediationActionRepository = remediationActionRepository;
        this.remediationActionMapper = remediationActionMapper;
    }

    /**
     * Save a remediationAction.
     *
     * @param remediationActionDTO the entity to save.
     * @return the persisted entity.
     */
    public RemediationActionDTO save(RemediationActionDTO remediationActionDTO) {
        LOG.debug("Request to save RemediationAction : {}", remediationActionDTO);
        RemediationAction remediationAction = remediationActionMapper.toEntity(remediationActionDTO);
        remediationAction = remediationActionRepository.save(remediationAction);
        return remediationActionMapper.toDto(remediationAction);
    }

    /**
     * Update a remediationAction.
     *
     * @param remediationActionDTO the entity to save.
     * @return the persisted entity.
     */
    public RemediationActionDTO update(RemediationActionDTO remediationActionDTO) {
        LOG.debug("Request to update RemediationAction : {}", remediationActionDTO);
        RemediationAction remediationAction = remediationActionMapper.toEntity(remediationActionDTO);
        remediationAction = remediationActionRepository.save(remediationAction);
        return remediationActionMapper.toDto(remediationAction);
    }

    /**
     * Partially update a remediationAction.
     *
     * @param remediationActionDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<RemediationActionDTO> partialUpdate(RemediationActionDTO remediationActionDTO) {
        LOG.debug("Request to partially update RemediationAction : {}", remediationActionDTO);

        return remediationActionRepository
            .findById(remediationActionDTO.getId())
            .map(existingRemediationAction -> {
                remediationActionMapper.partialUpdate(existingRemediationAction, remediationActionDTO);

                return existingRemediationAction;
            })
            .map(remediationActionRepository::save)
            .map(remediationActionMapper::toDto);
    }

    /**
     * Get one remediationAction by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<RemediationActionDTO> findOne(UUID id) {
        LOG.debug("Request to get RemediationAction : {}", id);
        return remediationActionRepository.findById(id).map(remediationActionMapper::toDto);
    }

    /**
     * Delete the remediationAction by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete RemediationAction : {}", id);
        remediationActionRepository.deleteById(id);
    }
}
