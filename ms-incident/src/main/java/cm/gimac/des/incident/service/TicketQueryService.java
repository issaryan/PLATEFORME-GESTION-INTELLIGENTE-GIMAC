package cm.gimac.des.incident.service;

import cm.gimac.des.incident.domain.*; // for static metamodels
import cm.gimac.des.incident.domain.Ticket;
import cm.gimac.des.incident.repository.TicketRepository;
import cm.gimac.des.incident.service.criteria.TicketCriteria;
import cm.gimac.des.incident.service.dto.TicketDTO;
import cm.gimac.des.incident.service.mapper.TicketMapper;
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
 * Service for executing complex queries for {@link Ticket} entities in the database.
 * The main input is a {@link TicketCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link TicketDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class TicketQueryService extends QueryService<Ticket> {

    private static final Logger LOG = LoggerFactory.getLogger(TicketQueryService.class);

    private final TicketRepository ticketRepository;

    private final TicketMapper ticketMapper;

    public TicketQueryService(TicketRepository ticketRepository, TicketMapper ticketMapper) {
        this.ticketRepository = ticketRepository;
        this.ticketMapper = ticketMapper;
    }

    /**
     * Return a {@link Page} of {@link TicketDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<TicketDTO> findByCriteria(TicketCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Ticket> specification = createSpecification(criteria);
        return ticketRepository.findAll(specification, page).map(ticketMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(TicketCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Ticket> specification = createSpecification(criteria);
        return ticketRepository.count(specification);
    }

    /**
     * Function to convert {@link TicketCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Ticket> createSpecification(TicketCriteria criteria) {
        Specification<Ticket> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), Ticket_.id),
                buildStringSpecification(criteria.getTicketNumber(), Ticket_.ticketNumber),
                buildSpecification(criteria.getType(), Ticket_.type),
                buildStringSpecification(criteria.getSubject(), Ticket_.subject),
                buildSpecification(criteria.getOrigin(), Ticket_.origin),
                buildStringSpecification(criteria.getOriginRef(), Ticket_.originRef),
                buildSpecification(criteria.getRequesterId(), Ticket_.requesterId),
                buildSpecification(criteria.getInstitutionId(), Ticket_.institutionId),
                buildStringSpecification(criteria.getLinkId(), Ticket_.linkId),
                buildSpecification(criteria.getAlertId(), Ticket_.alertId),
                buildRangeSpecification(criteria.getPriority(), Ticket_.priority),
                buildSpecification(criteria.getState(), Ticket_.state),
                buildSpecification(criteria.getAssigneeId(), Ticket_.assigneeId),
                buildRangeSpecification(criteria.getOpenedAt(), Ticket_.openedAt),
                buildRangeSpecification(criteria.getResolvedAt(), Ticket_.resolvedAt),
                buildSpecification(criteria.getResolverId(), Ticket_.resolverId),
                buildRangeSpecification(criteria.getClosedAt(), Ticket_.closedAt),
                buildSpecification(criteria.getClosedBy(), Ticket_.closedBy),
                buildRangeSpecification(criteria.getVersion(), Ticket_.version),
                buildSpecification(criteria.getRelatedTicketId(), root -> root.join(Ticket_.relatedTicket, JoinType.LEFT).get(Ticket_.id)),
                buildSpecification(criteria.getIncidentId(), root -> root.join(Ticket_.incident, JoinType.LEFT).get(Incident_.id))
            );
        }
        return specification;
    }
}
