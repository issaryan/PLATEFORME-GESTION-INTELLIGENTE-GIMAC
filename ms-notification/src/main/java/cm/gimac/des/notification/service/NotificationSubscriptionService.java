package cm.gimac.des.notification.service;

import cm.gimac.des.notification.domain.NotificationSubscription;
import cm.gimac.des.notification.repository.NotificationSubscriptionRepository;
import cm.gimac.des.notification.service.dto.NotificationSubscriptionDTO;
import cm.gimac.des.notification.service.mapper.NotificationSubscriptionMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.notification.domain.NotificationSubscription}.
 */
@Service
@Transactional
public class NotificationSubscriptionService {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationSubscriptionService.class);

    private final NotificationSubscriptionRepository notificationSubscriptionRepository;

    private final NotificationSubscriptionMapper notificationSubscriptionMapper;

    public NotificationSubscriptionService(
        NotificationSubscriptionRepository notificationSubscriptionRepository,
        NotificationSubscriptionMapper notificationSubscriptionMapper
    ) {
        this.notificationSubscriptionRepository = notificationSubscriptionRepository;
        this.notificationSubscriptionMapper = notificationSubscriptionMapper;
    }

    /**
     * Save a notificationSubscription.
     *
     * @param notificationSubscriptionDTO the entity to save.
     * @return the persisted entity.
     */
    public NotificationSubscriptionDTO save(NotificationSubscriptionDTO notificationSubscriptionDTO) {
        LOG.debug("Request to save NotificationSubscription : {}", notificationSubscriptionDTO);
        NotificationSubscription notificationSubscription = notificationSubscriptionMapper.toEntity(notificationSubscriptionDTO);
        notificationSubscription = notificationSubscriptionRepository.save(notificationSubscription);
        return notificationSubscriptionMapper.toDto(notificationSubscription);
    }

    /**
     * Update a notificationSubscription.
     *
     * @param notificationSubscriptionDTO the entity to save.
     * @return the persisted entity.
     */
    public NotificationSubscriptionDTO update(NotificationSubscriptionDTO notificationSubscriptionDTO) {
        LOG.debug("Request to update NotificationSubscription : {}", notificationSubscriptionDTO);
        NotificationSubscription notificationSubscription = notificationSubscriptionMapper.toEntity(notificationSubscriptionDTO);
        notificationSubscription = notificationSubscriptionRepository.save(notificationSubscription);
        return notificationSubscriptionMapper.toDto(notificationSubscription);
    }

    /**
     * Partially update a notificationSubscription.
     *
     * @param notificationSubscriptionDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<NotificationSubscriptionDTO> partialUpdate(NotificationSubscriptionDTO notificationSubscriptionDTO) {
        LOG.debug("Request to partially update NotificationSubscription : {}", notificationSubscriptionDTO);

        return notificationSubscriptionRepository
            .findById(notificationSubscriptionDTO.getId())
            .map(existingNotificationSubscription -> {
                notificationSubscriptionMapper.partialUpdate(existingNotificationSubscription, notificationSubscriptionDTO);

                return existingNotificationSubscription;
            })
            .map(notificationSubscriptionRepository::save)
            .map(notificationSubscriptionMapper::toDto);
    }

    /**
     * Get one notificationSubscription by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<NotificationSubscriptionDTO> findOne(UUID id) {
        LOG.debug("Request to get NotificationSubscription : {}", id);
        return notificationSubscriptionRepository.findById(id).map(notificationSubscriptionMapper::toDto);
    }

    /**
     * Delete the notificationSubscription by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete NotificationSubscription : {}", id);
        notificationSubscriptionRepository.deleteById(id);
    }
}
