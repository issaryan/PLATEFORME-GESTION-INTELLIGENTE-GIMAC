package cm.gimac.des.pilotage.service;

import cm.gimac.des.pilotage.domain.CollaboratorProjection;
import cm.gimac.des.pilotage.repository.CollaboratorProjectionRepository;
import cm.gimac.des.pilotage.service.dto.CollaboratorProjectionDTO;
import cm.gimac.des.pilotage.service.mapper.CollaboratorProjectionMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.pilotage.domain.CollaboratorProjection}.
 */
@Service
@Transactional
public class CollaboratorProjectionService {

    private static final Logger LOG = LoggerFactory.getLogger(CollaboratorProjectionService.class);

    private final CollaboratorProjectionRepository collaboratorProjectionRepository;

    private final CollaboratorProjectionMapper collaboratorProjectionMapper;

    public CollaboratorProjectionService(
        CollaboratorProjectionRepository collaboratorProjectionRepository,
        CollaboratorProjectionMapper collaboratorProjectionMapper
    ) {
        this.collaboratorProjectionRepository = collaboratorProjectionRepository;
        this.collaboratorProjectionMapper = collaboratorProjectionMapper;
    }

    /**
     * Save a collaboratorProjection.
     *
     * @param collaboratorProjectionDTO the entity to save.
     * @return the persisted entity.
     */
    public CollaboratorProjectionDTO save(CollaboratorProjectionDTO collaboratorProjectionDTO) {
        LOG.debug("Request to save CollaboratorProjection : {}", collaboratorProjectionDTO);
        CollaboratorProjection collaboratorProjection = collaboratorProjectionMapper.toEntity(collaboratorProjectionDTO);
        collaboratorProjection = collaboratorProjectionRepository.save(collaboratorProjection);
        return collaboratorProjectionMapper.toDto(collaboratorProjection);
    }

    /**
     * Update a collaboratorProjection.
     *
     * @param collaboratorProjectionDTO the entity to save.
     * @return the persisted entity.
     */
    public CollaboratorProjectionDTO update(CollaboratorProjectionDTO collaboratorProjectionDTO) {
        LOG.debug("Request to update CollaboratorProjection : {}", collaboratorProjectionDTO);
        CollaboratorProjection collaboratorProjection = collaboratorProjectionMapper.toEntity(collaboratorProjectionDTO);
        collaboratorProjection = collaboratorProjectionRepository.save(collaboratorProjection);
        return collaboratorProjectionMapper.toDto(collaboratorProjection);
    }

    /**
     * Partially update a collaboratorProjection.
     *
     * @param collaboratorProjectionDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<CollaboratorProjectionDTO> partialUpdate(CollaboratorProjectionDTO collaboratorProjectionDTO) {
        LOG.debug("Request to partially update CollaboratorProjection : {}", collaboratorProjectionDTO);

        return collaboratorProjectionRepository
            .findById(collaboratorProjectionDTO.getId())
            .map(existingCollaboratorProjection -> {
                collaboratorProjectionMapper.partialUpdate(existingCollaboratorProjection, collaboratorProjectionDTO);

                return existingCollaboratorProjection;
            })
            .map(collaboratorProjectionRepository::save)
            .map(collaboratorProjectionMapper::toDto);
    }

    /**
     * Get one collaboratorProjection by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<CollaboratorProjectionDTO> findOne(UUID id) {
        LOG.debug("Request to get CollaboratorProjection : {}", id);
        return collaboratorProjectionRepository.findById(id).map(collaboratorProjectionMapper::toDto);
    }

    /**
     * Delete the collaboratorProjection by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete CollaboratorProjection : {}", id);
        collaboratorProjectionRepository.deleteById(id);
    }
}
