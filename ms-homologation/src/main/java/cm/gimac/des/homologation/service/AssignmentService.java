package cm.gimac.des.homologation.service;

import cm.gimac.des.homologation.service.dto.AssignmentDTO;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service Interface for managing {@link cm.gimac.des.homologation.domain.Assignment}.
 */
public interface AssignmentService {
    /**
     * Save a assignment.
     *
     * @param assignmentDTO the entity to save.
     * @return the persisted entity.
     */
    AssignmentDTO save(AssignmentDTO assignmentDTO);

    /**
     * Updates a assignment.
     *
     * @param assignmentDTO the entity to update.
     * @return the persisted entity.
     */
    AssignmentDTO update(AssignmentDTO assignmentDTO);

    /**
     * Partially updates a assignment.
     *
     * @param assignmentDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<AssignmentDTO> partialUpdate(AssignmentDTO assignmentDTO);

    /**
     * Get all the assignments.
     *
     * @return the list of entities.
     */
    List<AssignmentDTO> findAll();

    /**
     * Get the "id" assignment.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<AssignmentDTO> findOne(UUID id);

    /**
     * Delete the "id" assignment.
     *
     * @param id the id of the entity.
     */
    void delete(UUID id);
}
