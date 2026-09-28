package cm.gimac.des.notification.service;

import cm.gimac.des.notification.domain.*; // for static metamodels
import cm.gimac.des.notification.domain.NotificationRecipient;
import cm.gimac.des.notification.repository.NotificationRecipientRepository;
import cm.gimac.des.notification.service.criteria.NotificationRecipientCriteria;
import cm.gimac.des.notification.service.dto.NotificationRecipientDTO;
import cm.gimac.des.notification.service.mapper.NotificationRecipientMapper;
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
 * Service for executing complex queries for {@link NotificationRecipient} entities in the database.
 * The main input is a {@link NotificationRecipientCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link NotificationRecipientDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class NotificationRecipientQueryService extends QueryService<NotificationRecipient> {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationRecipientQueryService.class);

    private final NotificationRecipientRepository notificationRecipientRepository;

    private final NotificationRecipientMapper notificationRecipientMapper;

    public NotificationRecipientQueryService(
        NotificationRecipientRepository notificationRecipientRepository,
        NotificationRecipientMapper notificationRecipientMapper
    ) {
        this.notificationRecipientRepository = notificationRecipientRepository;
        this.notificationRecipientMapper = notificationRecipientMapper;
    }

    /**
     * Return a {@link Page} of {@link NotificationRecipientDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<NotificationRecipientDTO> findByCriteria(NotificationRecipientCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<NotificationRecipient> specification = createSpecification(criteria);
        return notificationRecipientRepository.findAll(specification, page).map(notificationRecipientMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(NotificationRecipientCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<NotificationRecipient> specification = createSpecification(criteria);
        return notificationRecipientRepository.count(specification);
    }

    /**
     * Function to convert {@link NotificationRecipientCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<NotificationRecipient> createSpecification(NotificationRecipientCriteria criteria) {
        Specification<NotificationRecipient> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), NotificationRecipient_.id),
                buildSpecification(criteria.getUserId(), NotificationRecipient_.userId),
                buildSpecification(criteria.getInstitutionId(), NotificationRecipient_.institutionId),
                buildSpecification(criteria.getResolvedBy(), NotificationRecipient_.resolvedBy),
                buildStringSpecification(criteria.getChannels(), NotificationRecipient_.channels),
                buildSpecification(criteria.getEscalated(), NotificationRecipient_.escalated),
                buildRangeSpecification(criteria.getResolvedAt(), NotificationRecipient_.resolvedAt),
                buildRangeSpecification(criteria.getReadAt(), NotificationRecipient_.readAt),
                buildRangeSpecification(criteria.getAcknowledgedAt(), NotificationRecipient_.acknowledgedAt),
                buildSpecification(criteria.getNotificationId(), root ->
                    root.join(NotificationRecipient_.notification, JoinType.LEFT).get(Notification_.id)
                ),
                buildSpecification(criteria.getSourceSubscriptionId(), root ->
                    root.join(NotificationRecipient_.sourceSubscription, JoinType.LEFT).get(NotificationSubscription_.id)
                )
            );
        }
        return specification;
    }
}
