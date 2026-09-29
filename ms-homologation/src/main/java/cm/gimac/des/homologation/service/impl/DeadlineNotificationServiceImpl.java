package cm.gimac.des.homologation.service.impl;

import cm.gimac.des.homologation.domain.DeadlineNotification;
import cm.gimac.des.homologation.repository.DeadlineNotificationRepository;
import cm.gimac.des.homologation.service.DeadlineNotificationService;
import cm.gimac.des.homologation.service.dto.DeadlineNotificationDTO;
import cm.gimac.des.homologation.service.mapper.DeadlineNotificationMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.homologation.domain.DeadlineNotification}.
 */
@Service
@Transactional
public class DeadlineNotificationServiceImpl implements DeadlineNotificationService {

    private static final Logger LOG = LoggerFactory.getLogger(DeadlineNotificationServiceImpl.class);

    private final DeadlineNotificationRepository deadlineNotificationRepository;

    private final DeadlineNotificationMapper deadlineNotificationMapper;

    public DeadlineNotificationServiceImpl(
        DeadlineNotificationRepository deadlineNotificationRepository,
        DeadlineNotificationMapper deadlineNotificationMapper
    ) {
        this.deadlineNotificationRepository = deadlineNotificationRepository;
        this.deadlineNotificationMapper = deadlineNotificationMapper;
    }

    @Override
    public DeadlineNotificationDTO save(DeadlineNotificationDTO deadlineNotificationDTO) {
        LOG.debug("Request to save DeadlineNotification : {}", deadlineNotificationDTO);
        DeadlineNotification deadlineNotification = deadlineNotificationMapper.toEntity(deadlineNotificationDTO);
        deadlineNotification = deadlineNotificationRepository.save(deadlineNotification);
        return deadlineNotificationMapper.toDto(deadlineNotification);
    }

    @Override
    public DeadlineNotificationDTO update(DeadlineNotificationDTO deadlineNotificationDTO) {
        LOG.debug("Request to update DeadlineNotification : {}", deadlineNotificationDTO);
        DeadlineNotification deadlineNotification = deadlineNotificationMapper.toEntity(deadlineNotificationDTO);
        deadlineNotification = deadlineNotificationRepository.save(deadlineNotification);
        return deadlineNotificationMapper.toDto(deadlineNotification);
    }

    @Override
    public Optional<DeadlineNotificationDTO> partialUpdate(DeadlineNotificationDTO deadlineNotificationDTO) {
        LOG.debug("Request to partially update DeadlineNotification : {}", deadlineNotificationDTO);

        return deadlineNotificationRepository
            .findById(deadlineNotificationDTO.getId())
            .map(existingDeadlineNotification -> {
                deadlineNotificationMapper.partialUpdate(existingDeadlineNotification, deadlineNotificationDTO);

                return existingDeadlineNotification;
            })
            .map(deadlineNotificationRepository::save)
            .map(deadlineNotificationMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeadlineNotificationDTO> findAll() {
        LOG.debug("Request to get all DeadlineNotifications");
        return deadlineNotificationRepository
            .findAll()
            .stream()
            .map(deadlineNotificationMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DeadlineNotificationDTO> findOne(UUID id) {
        LOG.debug("Request to get DeadlineNotification : {}", id);
        return deadlineNotificationRepository.findById(id).map(deadlineNotificationMapper::toDto);
    }

    @Override
    public void delete(UUID id) {
        LOG.debug("Request to delete DeadlineNotification : {}", id);
        deadlineNotificationRepository.deleteById(id);
    }
}
