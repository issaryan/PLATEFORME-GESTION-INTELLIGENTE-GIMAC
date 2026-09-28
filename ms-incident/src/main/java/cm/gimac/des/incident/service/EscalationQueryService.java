package cm.gimac.des.incident.service;

import cm.gimac.des.incident.domain.*; // for static metamodels
import cm.gimac.des.incident.domain.Escalation;
import cm.gimac.des.incident.repository.EscalationRepository;
import cm.gimac.des.incident.service.criteria.EscalationCriteria;
import cm.gimac.des.incident.service.dto.EscalationDTO;
import cm.gimac.des.incident.service.mapper.EscalationMapper;
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
 * Service for executing complex queries for {@link Escalation} entities in the database.
 * The main input is a {@link EscalationCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link EscalationDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class EscalationQueryService extends QueryService<Escalation> {

    private static final Logger LOG = LoggerFactory.getLogger(EscalationQueryService.class);

    private final EscalationRepository escalationRepository;

    private final EscalationMapper escalationMapper;

    public EscalationQueryService(EscalationRepository escalationRepository, EscalationMapper escalationMapper) {
        this.escalationRepository = escalationRepository;
        this.escalationMapper = escalationMapper;
    }

    /**
     * Return a {@link Page} of {@link EscalationDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<EscalationDTO> findByCriteria(EscalationCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Escalation> specification = createSpecification(criteria);
        return escalationRepository.findAll(specification, page).map(escalationMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(EscalationCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Escalation> specification = createSpecification(criteria);
        return escalationRepository.count(specification);
    }

    /**
     * Function to convert {@link EscalationCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Escalation> createSpecification(EscalationCriteria criteria) {
        Specification<Escalation> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), Escalation_.id),
                buildSpecification(criteria.getScope(), Escalation_.scope),
                buildRangeSpecification(criteria.getLevel(), Escalation_.level),
                buildSpecification(criteria.getTargetUserId(), Escalation_.targetUserId),
                buildSpecification(criteria.getTargetContactId(), Escalation_.targetContactId),
                buildRangeSpecification(criteria.getTriggeredAt(), Escalation_.triggeredAt),
                buildStringSpecification(criteria.getReason(), Escalation_.reason),
                buildRangeSpecification(criteria.getImputableDelay(), Escalation_.imputableDelay),
                buildSpecification(criteria.getTicketId(), root -> root.join(Escalation_.ticket, JoinType.LEFT).get(Ticket_.id))
            );
        }
        return specification;
    }
}
