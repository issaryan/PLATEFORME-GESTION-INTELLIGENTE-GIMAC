package cm.gimac.des.iam.service;

import cm.gimac.des.iam.domain.Collaborator;
import cm.gimac.des.iam.repository.CollaboratorRepository;
import cm.gimac.des.iam.service.dto.CollaboratorDTO;
import cm.gimac.des.iam.service.mapper.CollaboratorMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.iam.domain.Collaborator}.
 */
@Service
@Transactional
public class CollaboratorService {

    private static final Logger LOG = LoggerFactory.getLogger(CollaboratorService.class);

    private final CollaboratorRepository collaboratorRepository;

    private final CollaboratorMapper collaboratorMapper;

    public CollaboratorService(CollaboratorRepository collaboratorRepository, CollaboratorMapper collaboratorMapper) {
        this.collaboratorRepository = collaboratorRepository;
        this.collaboratorMapper = collaboratorMapper;
    }

    /**
     * Save a collaborator.
     *
     * @param collaboratorDTO the entity to save.
     * @return the persisted entity.
     */
    public CollaboratorDTO save(CollaboratorDTO collaboratorDTO) {
        LOG.debug("Request to save Collaborator : {}", collaboratorDTO);
        Collaborator collaborator = collaboratorMapper.toEntity(collaboratorDTO);
        collaborator = collaboratorRepository.save(collaborator);
        return collaboratorMapper.toDto(collaborator);
    }

    /**
     * Update a collaborator.
     *
     * @param collaboratorDTO the entity to save.
     * @return the persisted entity.
     */
    public CollaboratorDTO update(CollaboratorDTO collaboratorDTO) {
        LOG.debug("Request to update Collaborator : {}", collaboratorDTO);
        Collaborator collaborator = collaboratorMapper.toEntity(collaboratorDTO);
        collaborator = collaboratorRepository.save(collaborator);
        return collaboratorMapper.toDto(collaborator);
    }

    /**
     * Partially update a collaborator.
     *
     * @param collaboratorDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<CollaboratorDTO> partialUpdate(CollaboratorDTO collaboratorDTO) {
        LOG.debug("Request to partially update Collaborator : {}", collaboratorDTO);

        return collaboratorRepository
            .findById(collaboratorDTO.getId())
            .map(existingCollaborator -> {
                collaboratorMapper.partialUpdate(existingCollaborator, collaboratorDTO);

                return existingCollaborator;
            })
            .map(collaboratorRepository::save)
            .map(collaboratorMapper::toDto);
    }

    /**
     * Get one collaborator by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<CollaboratorDTO> findOne(UUID id) {
        LOG.debug("Request to get Collaborator : {}", id);
        return collaboratorRepository.findById(id).map(collaboratorMapper::toDto);
    }

    /**
     * Delete the collaborator by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete Collaborator : {}", id);
        collaboratorRepository.deleteById(id);
    }
}
