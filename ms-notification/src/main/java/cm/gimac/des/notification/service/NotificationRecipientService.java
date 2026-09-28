package cm.gimac.des.notification.service;

import cm.gimac.des.notification.domain.NotificationRecipient;
import cm.gimac.des.notification.repository.NotificationRecipientRepository;
import cm.gimac.des.notification.service.dto.NotificationRecipientDTO;
import cm.gimac.des.notification.service.mapper.NotificationRecipientMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.notification.domain.NotificationRecipient}.
 */
@Service
@Transactional
public class NotificationRecipientService {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationRecipientService.class);

    private final NotificationRecipientRepository notificationRecipientRepository;

    private final NotificationRecipientMapper notificationRecipientMapper;

    public NotificationRecipientService(
        NotificationRecipientRepository notificationRecipientRepository,
        NotificationRecipientMapper notificationRecipientMapper
    ) {
        this.notificationRecipientRepository = notificationRecipientRepository;
        this.notificationRecipientMapper = notificationRecipientMapper;
    }

    /**
     * Save a notificationRecipient.
     *
     * @param notificationRecipientDTO the entity to save.
     * @return the persisted entity.
     */
    public NotificationRecipientDTO save(NotificationRecipientDTO notificationRecipientDTO) {
        LOG.debug("Request to save NotificationRecipient : {}", notificationRecipientDTO);
        NotificationRecipient notificationRecipient = notificationRecipientMapper.toEntity(notificationRecipientDTO);
        notificationRecipient = notificationRecipientRepository.save(notificationRecipient);
        return notificationRecipientMapper.toDto(notificationRecipient);
    }

    /**
     * Update a notificationRecipient.
     *
     * @param notificationRecipientDTO the entity to save.
     * @return the persisted entity.
     */
    public NotificationRecipientDTO update(NotificationRecipientDTO notificationRecipientDTO) {
        LOG.debug("Request to update NotificationRecipient : {}", notificationRecipientDTO);
        NotificationRecipient notificationRecipient = notificationRecipientMapper.toEntity(notificationRecipientDTO);
        notificationRecipient = notificationRecipientRepository.save(notificationRecipient);
        return notificationRecipientMapper.toDto(notificationRecipient);
    }

    /**
     * Partially update a notificationRecipient.
     *
     * @param notificationRecipientDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<NotificationRecipientDTO> partialUpdate(NotificationRecipientDTO notificationRecipientDTO) {
        LOG.debug("Request to partially update NotificationRecipient : {}", notificationRecipientDTO);

        return notificationRecipientRepository
            .findById(notificationRecipientDTO.getId())
            .map(existingNotificationRecipient -> {
                notificationRecipientMapper.partialUpdate(existingNotificationRecipient, notificationRecipientDTO);

                return existingNotificationRecipient;
            })
            .map(notificationRecipientRepository::save)
            .map(notificationRecipientMapper::toDto);
    }

    /**
     * Get one notificationRecipient by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<NotificationRecipientDTO> findOne(UUID id) {
        LOG.debug("Request to get NotificationRecipient : {}", id);
        return notificationRecipientRepository.findById(id).map(notificationRecipientMapper::toDto);
    }

    /**
     * Delete the notificationRecipient by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete NotificationRecipient : {}", id);
        notificationRecipientRepository.deleteById(id);
    }
}
