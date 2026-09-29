package cm.gimac.des.homologation.service;

import cm.gimac.des.homologation.service.dto.DeadlineNotificationDTO;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service Interface for managing {@link cm.gimac.des.homologation.domain.DeadlineNotification}.
 */
public interface DeadlineNotificationService {
    /**
     * Save a deadlineNotification.
     *
     * @param deadlineNotificationDTO the entity to save.
     * @return the persisted entity.
     */
    DeadlineNotificationDTO save(DeadlineNotificationDTO deadlineNotificationDTO);

    /**
     * Updates a deadlineNotification.
     *
     * @param deadlineNotificationDTO the entity to update.
     * @return the persisted entity.
     */
    DeadlineNotificationDTO update(DeadlineNotificationDTO deadlineNotificationDTO);

    /**
     * Partially updates a deadlineNotification.
     *
     * @param deadlineNotificationDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<DeadlineNotificationDTO> partialUpdate(DeadlineNotificationDTO deadlineNotificationDTO);

    /**
     * Get all the deadlineNotifications.
     *
     * @return the list of entities.
     */
    List<DeadlineNotificationDTO> findAll();

    /**
     * Get the "id" deadlineNotification.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<DeadlineNotificationDTO> findOne(UUID id);

    /**
     * Delete the "id" deadlineNotification.
     *
     * @param id the id of the entity.
     */
    void delete(UUID id);
}
