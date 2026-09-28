package cm.gimac.des.notification.service;

import cm.gimac.des.notification.domain.*; // for static metamodels
import cm.gimac.des.notification.domain.Notification;
import cm.gimac.des.notification.repository.NotificationRepository;
import cm.gimac.des.notification.service.criteria.NotificationCriteria;
import cm.gimac.des.notification.service.dto.NotificationDTO;
import cm.gimac.des.notification.service.mapper.NotificationMapper;
import jakarta.persistence.criteria.JoinType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Service for executing complex queries for {@link Notification} entities in the database.
 * The main input is a {@link NotificationCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link NotificationDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class NotificationQueryService extends QueryService<Notification> {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationQueryService.class);

    private final NotificationRepository notificationRepository;

    private final NotificationMapper notificationMapper;

    public NotificationQueryService(NotificationRepository notificationRepository, NotificationMapper notificationMapper) {
        this.notificationRepository = notificationRepository;
        this.notificationMapper = notificationMapper;
    }

    /**
     * Return a {@link Page} of {@link NotificationDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<NotificationDTO> findByCriteria(NotificationCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Notification> specification = createSpecification(criteria);
        return notificationRepository.findAll(specification, page).map(notificationMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(NotificationCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Notification> specification = createSpecification(criteria);
        return notificationRepository.count(specification);
    }

    /**
     * Function to convert {@link NotificationCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Notification> createSpecification(NotificationCriteria criteria) {
        Specification<Notification> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), Notification_.id),
                buildStringSpecification(criteria.getNotificationType(), Notification_.notificationType),
                buildSpecification(criteria.getUrgency(), Notification_.urgency),
                buildStringSpecification(criteria.getSubject(), Notification_.subject),
                buildStringSpecification(criteria.getObjectType(), Notification_.objectType),
                buildStringSpecification(criteria.getObjectId(), Notification_.objectId),
                buildSpecification(criteria.getInstitutionId(), Notification_.institutionId),
                buildSpecification(criteria.getSourceEventId(), Notification_.sourceEventId),
                buildStringSpecification(criteria.getSourceService(), Notification_.sourceService),
                buildSpecification(criteria.getUrgentPort(), Notification_.urgentPort),
                buildSpecification(criteria.getCorrelationId(), Notification_.correlationId),
                buildStringSpecification(criteria.getGroupKey(), Notification_.groupKey),
                buildRangeSpecification(criteria.getRequestedAt(), Notification_.requestedAt),
                buildRangeSpecification(criteria.getAckDeadline(), Notification_.ackDeadline),
                buildRangeSpecification(criteria.getEscalationLevel(), Notification_.escalationLevel),
                buildSpecification(criteria.getState(), Notification_.state),
                buildSpecification(criteria.getAcknowledgedBy(), Notification_.acknowledgedBy),
                buildRangeSpecification(criteria.getAcknowledgedAt(), Notification_.acknowledgedAt),
                buildSpecification(criteria.getSummary(), Notification_.summary),
                buildRangeSpecification(criteria.getOccurrenceCount(), Notification_.occurrenceCount),
                buildSpecification(criteria.getConsolidationWindowId(), root ->
                    root.join(Notification_.consolidationWindow, JoinType.LEFT).get(ConsolidationWindow_.id)
                ),
                buildSpecification(criteria.getSummarizedWindowId(), root ->
                    root.join(Notification_.summarizedWindow, JoinType.LEFT).get(ConsolidationWindow_.id)
                )
            );
        }
        return specification;
    }
}
