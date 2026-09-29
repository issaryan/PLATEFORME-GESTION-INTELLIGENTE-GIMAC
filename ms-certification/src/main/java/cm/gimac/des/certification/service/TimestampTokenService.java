package cm.gimac.des.certification.service;

import cm.gimac.des.certification.service.dto.TimestampTokenDTO;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service Interface for managing {@link cm.gimac.des.certification.domain.TimestampToken}.
 */
public interface TimestampTokenService {
    /**
     * Save a timestampToken.
     *
     * @param timestampTokenDTO the entity to save.
     * @return the persisted entity.
     */
    TimestampTokenDTO save(TimestampTokenDTO timestampTokenDTO);

    /**
     * Updates a timestampToken.
     *
     * @param timestampTokenDTO the entity to update.
     * @return the persisted entity.
     */
    TimestampTokenDTO update(TimestampTokenDTO timestampTokenDTO);

    /**
     * Partially updates a timestampToken.
     *
     * @param timestampTokenDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<TimestampTokenDTO> partialUpdate(TimestampTokenDTO timestampTokenDTO);

    /**
     * Get all the timestampTokens.
     *
     * @return the list of entities.
     */
    List<TimestampTokenDTO> findAll();

    /**
     * Get the "id" timestampToken.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<TimestampTokenDTO> findOne(UUID id);

    /**
     * Delete the "id" timestampToken.
     *
     * @param id the id of the entity.
     */
    void delete(UUID id);
}
