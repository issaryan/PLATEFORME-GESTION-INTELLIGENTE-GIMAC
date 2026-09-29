package cm.gimac.des.certification.service;

import cm.gimac.des.certification.service.dto.QuitusDTO;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service Interface for managing {@link cm.gimac.des.certification.domain.Quitus}.
 */
public interface QuitusService {
    /**
     * Save a quitus.
     *
     * @param quitusDTO the entity to save.
     * @return the persisted entity.
     */
    QuitusDTO save(QuitusDTO quitusDTO);

    /**
     * Updates a quitus.
     *
     * @param quitusDTO the entity to update.
     * @return the persisted entity.
     */
    QuitusDTO update(QuitusDTO quitusDTO);

    /**
     * Partially updates a quitus.
     *
     * @param quitusDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<QuitusDTO> partialUpdate(QuitusDTO quitusDTO);

    /**
     * Get all the quituses.
     *
     * @return the list of entities.
     */
    List<QuitusDTO> findAll();

    /**
     * Get all the QuitusDTO where SignedDecision is {@code null}.
     *
     * @return the {@link List} of entities.
     */
    List<QuitusDTO> findAllWhereSignedDecisionIsNull();

    /**
     * Get the "id" quitus.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<QuitusDTO> findOne(UUID id);

    /**
     * Delete the "id" quitus.
     *
     * @param id the id of the entity.
     */
    void delete(UUID id);
}
