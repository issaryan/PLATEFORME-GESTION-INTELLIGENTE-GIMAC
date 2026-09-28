package cm.gimac.des.incident.service;

import cm.gimac.des.incident.domain.*; // for static metamodels
import cm.gimac.des.incident.domain.RemediationAction;
import cm.gimac.des.incident.repository.RemediationActionRepository;
import cm.gimac.des.incident.service.criteria.RemediationActionCriteria;
import cm.gimac.des.incident.service.dto.RemediationActionDTO;
import cm.gimac.des.incident.service.mapper.RemediationActionMapper;
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
 * Service for executing complex queries for {@link RemediationAction} entities in the database.
 * The main input is a {@link RemediationActionCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link RemediationActionDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class RemediationActionQueryService extends QueryService<RemediationAction> {

    private static final Logger LOG = LoggerFactory.getLogger(RemediationActionQueryService.class);

    private final RemediationActionRepository remediationActionRepository;

    private final RemediationActionMapper remediationActionMapper;

    public RemediationActionQueryService(
        RemediationActionRepository remediationActionRepository,
        RemediationActionMapper remediationActionMapper
    ) {
        this.remediationActionRepository = remediationActionRepository;
        this.remediationActionMapper = remediationActionMapper;
    }

    /**
     * Return a {@link Page} of {@link RemediationActionDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<RemediationActionDTO> findByCriteria(RemediationActionCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<RemediationAction> specification = createSpecification(criteria);
        return remediationActionRepository.findAll(specification, page).map(remediationActionMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(RemediationActionCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<RemediationAction> specification = createSpecification(criteria);
        return remediationActionRepository.count(specification);
    }

    /**
     * Function to convert {@link RemediationActionCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<RemediationAction> createSpecification(RemediationActionCriteria criteria) {
        Specification<RemediationAction> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), RemediationAction_.id),
                buildSpecification(criteria.getKind(), RemediationAction_.kind),
                buildStringSpecification(criteria.getDescription(), RemediationAction_.description),
                buildSpecification(criteria.getOwnerId(), RemediationAction_.ownerId),
                buildRangeSpecification(criteria.getDueDate(), RemediationAction_.dueDate),
                buildRangeSpecification(criteria.getRequestedAt(), RemediationAction_.requestedAt),
                buildStringSpecification(criteria.getTaskRef(), RemediationAction_.taskRef),
                buildSpecification(criteria.getState(), RemediationAction_.state),
                buildRangeSpecification(criteria.getCompletedAt(), RemediationAction_.completedAt),
                buildSpecification(criteria.getIncidentId(), root -> root.join(RemediationAction_.incident, JoinType.LEFT).get(Incident_.id)
                ),
                buildSpecification(criteria.getCrisisDecisionId(), root ->
                    root.join(RemediationAction_.crisisDecision, JoinType.LEFT).get(CrisisDecision_.id)
                )
            );
        }
        return specification;
    }
}
