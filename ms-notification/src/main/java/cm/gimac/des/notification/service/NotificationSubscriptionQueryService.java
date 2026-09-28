package cm.gimac.des.notification.service;

import cm.gimac.des.notification.domain.*; // for static metamodels
import cm.gimac.des.notification.domain.NotificationSubscription;
import cm.gimac.des.notification.repository.NotificationSubscriptionRepository;
import cm.gimac.des.notification.service.criteria.NotificationSubscriptionCriteria;
import cm.gimac.des.notification.service.dto.NotificationSubscriptionDTO;
import cm.gimac.des.notification.service.mapper.NotificationSubscriptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Service for executing complex queries for {@link NotificationSubscription} entities in the database.
 * The main input is a {@link NotificationSubscriptionCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link NotificationSubscriptionDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class NotificationSubscriptionQueryService extends QueryService<NotificationSubscription> {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationSubscriptionQueryService.class);

    private final NotificationSubscriptionRepository notificationSubscriptionRepository;

    private final NotificationSubscriptionMapper notificationSubscriptionMapper;

    public NotificationSubscriptionQueryService(
        NotificationSubscriptionRepository notificationSubscriptionRepository,
        NotificationSubscriptionMapper notificationSubscriptionMapper
    ) {
        this.notificationSubscriptionRepository = notificationSubscriptionRepository;
        this.notificationSubscriptionMapper = notificationSubscriptionMapper;
    }

    /**
     * Return a {@link Page} of {@link NotificationSubscriptionDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<NotificationSubscriptionDTO> findByCriteria(NotificationSubscriptionCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<NotificationSubscription> specification = createSpecification(criteria);
        return notificationSubscriptionRepository.findAll(specification, page).map(notificationSubscriptionMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(NotificationSubscriptionCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<NotificationSubscription> specification = createSpecification(criteria);
        return notificationSubscriptionRepository.count(specification);
    }

    /**
     * Function to convert {@link NotificationSubscriptionCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<NotificationSubscription> createSpecification(NotificationSubscriptionCriteria criteria) {
        Specification<NotificationSubscription> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), NotificationSubscription_.id),
                buildStringSpecification(criteria.getNotificationType(), NotificationSubscription_.notificationType),
                buildSpecification(criteria.getRuleType(), NotificationSubscription_.ruleType),
                buildStringSpecification(criteria.getRuleValue(), NotificationSubscription_.ruleValue),
                buildSpecification(criteria.getMinimumUrgency(), NotificationSubscription_.minimumUrgency),
                buildSpecification(criteria.getOnDutyOnly(), NotificationSubscription_.onDutyOnly),
                buildSpecification(criteria.getActive(), NotificationSubscription_.active),
                buildRangeSpecification(criteria.getRuleVersion(), NotificationSubscription_.ruleVersion),
                buildSpecification(criteria.getUpdatedBy(), NotificationSubscription_.updatedBy),
                buildRangeSpecification(criteria.getUpdatedAt(), NotificationSubscription_.updatedAt)
            );
        }
        return specification;
    }
}
