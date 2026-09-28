package cm.gimac.des.monitoring.service;

import cm.gimac.des.monitoring.domain.*; // for static metamodels
import cm.gimac.des.monitoring.domain.Shift;
import cm.gimac.des.monitoring.repository.ShiftRepository;
import cm.gimac.des.monitoring.service.criteria.ShiftCriteria;
import cm.gimac.des.monitoring.service.dto.ShiftDTO;
import cm.gimac.des.monitoring.service.mapper.ShiftMapper;
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
 * Service for executing complex queries for {@link Shift} entities in the database.
 * The main input is a {@link ShiftCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link ShiftDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class ShiftQueryService extends QueryService<Shift> {

    private static final Logger LOG = LoggerFactory.getLogger(ShiftQueryService.class);

    private final ShiftRepository shiftRepository;

    private final ShiftMapper shiftMapper;

    public ShiftQueryService(ShiftRepository shiftRepository, ShiftMapper shiftMapper) {
        this.shiftRepository = shiftRepository;
        this.shiftMapper = shiftMapper;
    }

    /**
     * Return a {@link Page} of {@link ShiftDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<ShiftDTO> findByCriteria(ShiftCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Shift> specification = createSpecification(criteria);
        return shiftRepository.findAll(specification, page).map(shiftMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(ShiftCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Shift> specification = createSpecification(criteria);
        return shiftRepository.count(specification);
    }

    /**
     * Function to convert {@link ShiftCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Shift> createSpecification(ShiftCriteria criteria) {
        Specification<Shift> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), Shift_.id),
                buildStringSpecification(criteria.getReference(), Shift_.reference),
                buildRangeSpecification(criteria.getShiftDate(), Shift_.shiftDate),
                buildSpecification(criteria.getSlot(), Shift_.slot),
                buildStringSpecification(criteria.getOperatorIds(), Shift_.operatorIds),
                buildSpecification(criteria.getShiftLeadId(), Shift_.shiftLeadId),
                buildRangeSpecification(criteria.getStartsAt(), Shift_.startsAt),
                buildRangeSpecification(criteria.getEndsAt(), Shift_.endsAt),
                buildSpecification(criteria.getRosterId(), root -> root.join(Shift_.roster, JoinType.LEFT).get(OnCallRoster_.id)),
                buildSpecification(criteria.getShiftReportId(), root -> root.join(Shift_.shiftReport, JoinType.LEFT).get(ShiftReport_.id))
            );
        }
        return specification;
    }
}
