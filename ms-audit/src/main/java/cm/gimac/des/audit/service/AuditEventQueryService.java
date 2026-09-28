package cm.gimac.des.audit.service;

import cm.gimac.des.audit.domain.*; // for static metamodels
import cm.gimac.des.audit.domain.AuditEvent;
import cm.gimac.des.audit.repository.AuditEventRepository;
import cm.gimac.des.audit.service.criteria.AuditEventCriteria;
import cm.gimac.des.audit.service.dto.AuditEventDTO;
import cm.gimac.des.audit.service.mapper.AuditEventMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Service for executing complex queries for {@link AuditEvent} entities in the database.
 * The main input is a {@link AuditEventCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link AuditEventDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class AuditEventQueryService extends QueryService<AuditEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(AuditEventQueryService.class);

    private final AuditEventRepository auditEventRepository;

    private final AuditEventMapper auditEventMapper;

    public AuditEventQueryService(AuditEventRepository auditEventRepository, AuditEventMapper auditEventMapper) {
        this.auditEventRepository = auditEventRepository;
        this.auditEventMapper = auditEventMapper;
    }

    /**
     * Return a {@link Page} of {@link AuditEventDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<AuditEventDTO> findByCriteria(AuditEventCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<AuditEvent> specification = createSpecification(criteria);
        return auditEventRepository.findAll(specification, page).map(auditEventMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(AuditEventCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<AuditEvent> specification = createSpecification(criteria);
        return auditEventRepository.count(specification);
    }

    /**
     * Function to convert {@link AuditEventCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<AuditEvent> createSpecification(AuditEventCriteria criteria) {
        Specification<AuditEvent> specification = Specification.unrestricted();
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = specification.and(
                Specification.allOf(
                    Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : Specification.unrestricted(),
                    buildSpecification(criteria.getId(), AuditEvent_.id),
                    buildRangeSpecification(criteria.getLogIndex(), AuditEvent_.logIndex),
                    buildSpecification(criteria.getEventId(), AuditEvent_.eventId),
                    buildRangeSpecification(criteria.getOccurredAt(), AuditEvent_.occurredAt),
                    buildRangeSpecification(criteria.getRecordedAt(), AuditEvent_.recordedAt),
                    buildStringSpecification(criteria.getActorId(), AuditEvent_.actorId),
                    buildStringSpecification(criteria.getActionType(), AuditEvent_.actionType),
                    buildStringSpecification(criteria.getDomain(), AuditEvent_.domain),
                    buildStringSpecification(criteria.getSubject(), AuditEvent_.subject),
                    buildSpecification(criteria.getCorrelationId(), AuditEvent_.correlationId),
                    buildStringSpecification(criteria.getLeafHash(), AuditEvent_.leafHash)
                )
            );
        }
        return specification;
    }
}
