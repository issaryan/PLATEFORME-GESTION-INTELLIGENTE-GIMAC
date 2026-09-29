package cm.gimac.des.homologation.service;

import cm.gimac.des.homologation.service.dto.OperationInstanceDTO;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing {@link cm.gimac.des.homologation.domain.OperationInstance}.
 */
public interface OperationInstanceService {
    /**
     * Save a operationInstance.
     *
     * @param operationInstanceDTO the entity to save.
     * @return the persisted entity.
     */
    OperationInstanceDTO save(OperationInstanceDTO operationInstanceDTO);

    /**
     * Updates a operationInstance.
     *
     * @param operationInstanceDTO the entity to update.
     * @return the persisted entity.
     */
    OperationInstanceDTO update(OperationInstanceDTO operationInstanceDTO);

    /**
     * Partially updates a operationInstance.
     *
     * @param operationInstanceDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<OperationInstanceDTO> partialUpdate(OperationInstanceDTO operationInstanceDTO);

    /**
     * Get all the operationInstances.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<OperationInstanceDTO> findAll(Pageable pageable);

    /**
     * Get the "id" operationInstance.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<OperationInstanceDTO> findOne(UUID id);

    /**
     * Delete the "id" operationInstance.
     *
     * @param id the id of the entity.
     */
    void delete(UUID id);
}
