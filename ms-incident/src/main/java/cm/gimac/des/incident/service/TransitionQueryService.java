package cm.gimac.des.incident.service;

import cm.gimac.des.incident.domain.*; // for static metamodels
import cm.gimac.des.incident.domain.Transition;
import cm.gimac.des.incident.repository.TransitionRepository;
import cm.gimac.des.incident.service.criteria.TransitionCriteria;
import cm.gimac.des.incident.service.dto.TransitionDTO;
import cm.gimac.des.incident.service.mapper.TransitionMapper;
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
 * Service for executing complex queries for {@link Transition} entities in the database.
 * The main input is a {@link TransitionCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link TransitionDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class TransitionQueryService extends QueryService<Transition> {

    private static final Logger LOG = LoggerFactory.getLogger(TransitionQueryService.class);

    private final TransitionRepository transitionRepository;

    private final TransitionMapper transitionMapper;

    public TransitionQueryService(TransitionRepository transitionRepository, TransitionMapper transitionMapper) {
        this.transitionRepository = transitionRepository;
        this.transitionMapper = transitionMapper;
    }

    /**
     * Return a {@link Page} of {@link TransitionDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<TransitionDTO> findByCriteria(TransitionCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Transition> specification = createSpecification(criteria);
        return transitionRepository.findAll(specification, page).map(transitionMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(TransitionCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Transition> specification = createSpecification(criteria);
        return transitionRepository.count(specification);
    }

    /**
     * Function to convert {@link TransitionCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Transition> createSpecification(TransitionCriteria criteria) {
        Specification<Transition> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), Transition_.id),
                buildRangeSpecification(criteria.getChainIndex(), Transition_.chainIndex),
                buildSpecification(criteria.getFromState(), Transition_.fromState),
                buildSpecification(criteria.getToState(), Transition_.toState),
                buildSpecification(criteria.getActorId(), Transition_.actorId),
                buildRangeSpecification(criteria.getOccurredAt(), Transition_.occurredAt),
                buildStringSpecification(criteria.getJustification(), Transition_.justification),
                buildStringSpecification(criteria.getPreviousHash(), Transition_.previousHash),
                buildStringSpecification(criteria.getHash(), Transition_.hash),
                buildSpecification(criteria.getTicketId(), root -> root.join(Transition_.ticket, JoinType.LEFT).get(Ticket_.id))
            );
        }
        return specification;
    }
}
