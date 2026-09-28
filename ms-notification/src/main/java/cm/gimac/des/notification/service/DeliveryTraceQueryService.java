package cm.gimac.des.notification.service;

import cm.gimac.des.notification.domain.*; // for static metamodels
import cm.gimac.des.notification.domain.DeliveryTrace;
import cm.gimac.des.notification.repository.DeliveryTraceRepository;
import cm.gimac.des.notification.service.criteria.DeliveryTraceCriteria;
import cm.gimac.des.notification.service.dto.DeliveryTraceDTO;
import cm.gimac.des.notification.service.mapper.DeliveryTraceMapper;
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
 * Service for executing complex queries for {@link DeliveryTrace} entities in the database.
 * The main input is a {@link DeliveryTraceCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link DeliveryTraceDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class DeliveryTraceQueryService extends QueryService<DeliveryTrace> {

    private static final Logger LOG = LoggerFactory.getLogger(DeliveryTraceQueryService.class);

    private final DeliveryTraceRepository deliveryTraceRepository;

    private final DeliveryTraceMapper deliveryTraceMapper;

    public DeliveryTraceQueryService(DeliveryTraceRepository deliveryTraceRepository, DeliveryTraceMapper deliveryTraceMapper) {
        this.deliveryTraceRepository = deliveryTraceRepository;
        this.deliveryTraceMapper = deliveryTraceMapper;
    }

    /**
     * Return a {@link Page} of {@link DeliveryTraceDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<DeliveryTraceDTO> findByCriteria(DeliveryTraceCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<DeliveryTrace> specification = createSpecification(criteria);
        return deliveryTraceRepository.findAll(specification, page).map(deliveryTraceMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(DeliveryTraceCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<DeliveryTrace> specification = createSpecification(criteria);
        return deliveryTraceRepository.count(specification);
    }

    /**
     * Function to convert {@link DeliveryTraceCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<DeliveryTrace> createSpecification(DeliveryTraceCriteria criteria) {
        Specification<DeliveryTrace> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), DeliveryTrace_.id),
                buildSpecification(criteria.getUserId(), DeliveryTrace_.userId),
                buildSpecification(criteria.getChannel(), DeliveryTrace_.channel),
                buildSpecification(criteria.getState(), DeliveryTrace_.state),
                buildRangeSpecification(criteria.getAttempts(), DeliveryTrace_.attempts),
                buildRangeSpecification(criteria.getNextAttemptAt(), DeliveryTrace_.nextAttemptAt),
                buildStringSpecification(criteria.getLastError(), DeliveryTrace_.lastError),
                buildStringSpecification(criteria.getProviderMessageId(), DeliveryTrace_.providerMessageId),
                buildRangeSpecification(criteria.getDeliveredAt(), DeliveryTrace_.deliveredAt),
                buildRangeSpecification(criteria.getAcknowledgedAt(), DeliveryTrace_.acknowledgedAt),
                buildSpecification(criteria.getNotificationId(), root ->
                    root.join(DeliveryTrace_.notification, JoinType.LEFT).get(Notification_.id)
                ),
                buildSpecification(criteria.getTemplateId(), root ->
                    root.join(DeliveryTrace_.template, JoinType.LEFT).get(MessageTemplate_.id)
                )
            );
        }
        return specification;
    }
}
