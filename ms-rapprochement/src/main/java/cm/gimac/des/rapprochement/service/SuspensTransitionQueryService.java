package cm.gimac.des.rapprochement.service;

import cm.gimac.des.rapprochement.domain.*; // for static metamodels
import cm.gimac.des.rapprochement.domain.SuspensTransition;
import cm.gimac.des.rapprochement.repository.SuspensTransitionRepository;
import cm.gimac.des.rapprochement.service.criteria.SuspensTransitionCriteria;
import cm.gimac.des.rapprochement.service.dto.SuspensTransitionDTO;
import cm.gimac.des.rapprochement.service.mapper.SuspensTransitionMapper;
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
 * Service for executing complex queries for {@link SuspensTransition} entities in the database.
 * The main input is a {@link SuspensTransitionCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link SuspensTransitionDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class SuspensTransitionQueryService extends QueryService<SuspensTransition> {

    private static final Logger LOG = LoggerFactory.getLogger(SuspensTransitionQueryService.class);

    private final SuspensTransitionRepository suspensTransitionRepository;

    private final SuspensTransitionMapper suspensTransitionMapper;

    public SuspensTransitionQueryService(
        SuspensTransitionRepository suspensTransitionRepository,
        SuspensTransitionMapper suspensTransitionMapper
    ) {
        this.suspensTransitionRepository = suspensTransitionRepository;
        this.suspensTransitionMapper = suspensTransitionMapper;
    }

    /**
     * Return a {@link Page} of {@link SuspensTransitionDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<SuspensTransitionDTO> findByCriteria(SuspensTransitionCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<SuspensTransition> specification = createSpecification(criteria);
        return suspensTransitionRepository.findAll(specification, page).map(suspensTransitionMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(SuspensTransitionCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<SuspensTransition> specification = createSpecification(criteria);
        return suspensTransitionRepository.count(specification);
    }

    /**
     * Function to convert {@link SuspensTransitionCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<SuspensTransition> createSpecification(SuspensTransitionCriteria criteria) {
        Specification<SuspensTransition> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), SuspensTransition_.id),
                buildSpecification(criteria.getFromState(), SuspensTransition_.fromState),
                buildSpecification(criteria.getToState(), SuspensTransition_.toState),
                buildSpecification(criteria.getActorId(), SuspensTransition_.actorId),
                buildRangeSpecification(criteria.getOccurredAt(), SuspensTransition_.occurredAt),
                buildStringSpecification(criteria.getJustification(), SuspensTransition_.justification),
                buildSpecification(criteria.getSuspensId(), root -> root.join(SuspensTransition_.suspens, JoinType.LEFT).get(Suspens_.id))
            );
        }
        return specification;
    }
}
