package cm.gimac.des.participant.service;

import cm.gimac.des.participant.domain.*; // for static metamodels
import cm.gimac.des.participant.domain.StatusTransition;
import cm.gimac.des.participant.repository.StatusTransitionRepository;
import cm.gimac.des.participant.service.criteria.StatusTransitionCriteria;
import cm.gimac.des.participant.service.dto.StatusTransitionDTO;
import cm.gimac.des.participant.service.mapper.StatusTransitionMapper;
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
 * Service for executing complex queries for {@link StatusTransition} entities in the database.
 * The main input is a {@link StatusTransitionCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link StatusTransitionDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class StatusTransitionQueryService extends QueryService<StatusTransition> {

    private static final Logger LOG = LoggerFactory.getLogger(StatusTransitionQueryService.class);

    private final StatusTransitionRepository statusTransitionRepository;

    private final StatusTransitionMapper statusTransitionMapper;

    public StatusTransitionQueryService(
        StatusTransitionRepository statusTransitionRepository,
        StatusTransitionMapper statusTransitionMapper
    ) {
        this.statusTransitionRepository = statusTransitionRepository;
        this.statusTransitionMapper = statusTransitionMapper;
    }

    /**
     * Return a {@link Page} of {@link StatusTransitionDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<StatusTransitionDTO> findByCriteria(StatusTransitionCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<StatusTransition> specification = createSpecification(criteria);
        return statusTransitionRepository.findAll(specification, page).map(statusTransitionMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(StatusTransitionCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<StatusTransition> specification = createSpecification(criteria);
        return statusTransitionRepository.count(specification);
    }

    /**
     * Function to convert {@link StatusTransitionCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<StatusTransition> createSpecification(StatusTransitionCriteria criteria) {
        Specification<StatusTransition> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), StatusTransition_.id),
                buildSpecification(criteria.getFromStatus(), StatusTransition_.fromStatus),
                buildSpecification(criteria.getToStatus(), StatusTransition_.toStatus),
                buildStringSpecification(criteria.getReason(), StatusTransition_.reason),
                buildSpecification(criteria.getRequesterId(), StatusTransition_.requesterId),
                buildRangeSpecification(criteria.getRequestedAt(), StatusTransition_.requestedAt),
                buildSpecification(criteria.getAutomaticTransition(), StatusTransition_.automaticTransition),
                buildRangeSpecification(criteria.getRequiredApprovals(), StatusTransition_.requiredApprovals),
                buildSpecification(criteria.getApproverId(), StatusTransition_.approverId),
                buildSpecification(criteria.getSecondApproverId(), StatusTransition_.secondApproverId),
                buildRangeSpecification(criteria.getSecondApprovedAt(), StatusTransition_.secondApprovedAt),
                buildRangeSpecification(criteria.getDecidedAt(), StatusTransition_.decidedAt),
                buildSpecification(criteria.getState(), StatusTransition_.state),
                buildStringSpecification(criteria.getRejectionReason(), StatusTransition_.rejectionReason),
                buildSpecification(criteria.getEvidenceDocumentId(), StatusTransition_.evidenceDocumentId),
                buildSpecification(criteria.getInstitutionId(), root ->
                    root.join(StatusTransition_.institution, JoinType.LEFT).get(Institution_.id)
                )
            );
        }
        return specification;
    }
}
