package cm.gimac.des.monitoring.service;

import cm.gimac.des.monitoring.domain.*; // for static metamodels
import cm.gimac.des.monitoring.domain.OnCallAssignment;
import cm.gimac.des.monitoring.repository.OnCallAssignmentRepository;
import cm.gimac.des.monitoring.service.criteria.OnCallAssignmentCriteria;
import cm.gimac.des.monitoring.service.dto.OnCallAssignmentDTO;
import cm.gimac.des.monitoring.service.mapper.OnCallAssignmentMapper;
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
 * Service for executing complex queries for {@link OnCallAssignment} entities in the database.
 * The main input is a {@link OnCallAssignmentCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link OnCallAssignmentDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class OnCallAssignmentQueryService extends QueryService<OnCallAssignment> {

    private static final Logger LOG = LoggerFactory.getLogger(OnCallAssignmentQueryService.class);

    private final OnCallAssignmentRepository onCallAssignmentRepository;

    private final OnCallAssignmentMapper onCallAssignmentMapper;

    public OnCallAssignmentQueryService(
        OnCallAssignmentRepository onCallAssignmentRepository,
        OnCallAssignmentMapper onCallAssignmentMapper
    ) {
        this.onCallAssignmentRepository = onCallAssignmentRepository;
        this.onCallAssignmentMapper = onCallAssignmentMapper;
    }

    /**
     * Return a {@link Page} of {@link OnCallAssignmentDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<OnCallAssignmentDTO> findByCriteria(OnCallAssignmentCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<OnCallAssignment> specification = createSpecification(criteria);
        return onCallAssignmentRepository.findAll(specification, page).map(onCallAssignmentMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(OnCallAssignmentCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<OnCallAssignment> specification = createSpecification(criteria);
        return onCallAssignmentRepository.count(specification);
    }

    /**
     * Function to convert {@link OnCallAssignmentCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<OnCallAssignment> createSpecification(OnCallAssignmentCriteria criteria) {
        Specification<OnCallAssignment> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), OnCallAssignment_.id),
                buildRangeSpecification(criteria.getDutyDate(), OnCallAssignment_.dutyDate),
                buildSpecification(criteria.getSlot(), OnCallAssignment_.slot),
                buildRangeSpecification(criteria.getLevel(), OnCallAssignment_.level),
                buildSpecification(criteria.getOperatorId(), OnCallAssignment_.operatorId),
                buildSpecification(criteria.getShiftLead(), OnCallAssignment_.shiftLead),
                buildSpecification(criteria.getRosterId(), root -> root.join(OnCallAssignment_.roster, JoinType.LEFT).get(OnCallRoster_.id))
            );
        }
        return specification;
    }
}
