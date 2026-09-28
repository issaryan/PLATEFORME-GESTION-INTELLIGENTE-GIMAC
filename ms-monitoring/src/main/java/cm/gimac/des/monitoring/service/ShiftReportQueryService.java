package cm.gimac.des.monitoring.service;

import cm.gimac.des.monitoring.domain.*; // for static metamodels
import cm.gimac.des.monitoring.domain.ShiftReport;
import cm.gimac.des.monitoring.repository.ShiftReportRepository;
import cm.gimac.des.monitoring.service.criteria.ShiftReportCriteria;
import cm.gimac.des.monitoring.service.dto.ShiftReportDTO;
import cm.gimac.des.monitoring.service.mapper.ShiftReportMapper;
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
 * Service for executing complex queries for {@link ShiftReport} entities in the database.
 * The main input is a {@link ShiftReportCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link ShiftReportDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class ShiftReportQueryService extends QueryService<ShiftReport> {

    private static final Logger LOG = LoggerFactory.getLogger(ShiftReportQueryService.class);

    private final ShiftReportRepository shiftReportRepository;

    private final ShiftReportMapper shiftReportMapper;

    public ShiftReportQueryService(ShiftReportRepository shiftReportRepository, ShiftReportMapper shiftReportMapper) {
        this.shiftReportRepository = shiftReportRepository;
        this.shiftReportMapper = shiftReportMapper;
    }

    /**
     * Return a {@link Page} of {@link ShiftReportDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<ShiftReportDTO> findByCriteria(ShiftReportCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<ShiftReport> specification = createSpecification(criteria);
        return shiftReportRepository.findAll(specification, page).map(shiftReportMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(ShiftReportCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<ShiftReport> specification = createSpecification(criteria);
        return shiftReportRepository.count(specification);
    }

    /**
     * Function to convert {@link ShiftReportCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<ShiftReport> createSpecification(ShiftReportCriteria criteria) {
        Specification<ShiftReport> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), ShiftReport_.id),
                buildStringSpecification(criteria.getReference(), ShiftReport_.reference),
                buildSpecification(criteria.getState(), ShiftReport_.state),
                buildStringSpecification(criteria.getLogHeadHash(), ShiftReport_.logHeadHash),
                buildStringSpecification(criteria.getContentHash(), ShiftReport_.contentHash),
                buildStringSpecification(criteria.getDigest(), ShiftReport_.digest),
                buildStringSpecification(criteria.getDocumentDigest(), ShiftReport_.documentDigest),
                buildStringSpecification(criteria.getTimestampSerialNumber(), ShiftReport_.timestampSerialNumber),
                buildStringSpecification(criteria.getDocumentRef(), ShiftReport_.documentRef),
                buildRangeSpecification(criteria.getResponsibilityTransferredAt(), ShiftReport_.responsibilityTransferredAt),
                buildRangeSpecification(criteria.getSealedAt(), ShiftReport_.sealedAt),
                buildSpecification(criteria.getShiftId(), root -> root.join(ShiftReport_.shift, JoinType.LEFT).get(Shift_.id)),
                buildSpecification(criteria.getSupersedesId(), root ->
                    root.join(ShiftReport_.supersedes, JoinType.LEFT).get(ShiftReport_.id)
                ),
                buildSpecification(criteria.getShiftReportId(), root ->
                    root.join(ShiftReport_.shiftReport, JoinType.LEFT).get(ShiftReport_.id)
                )
            );
        }
        return specification;
    }
}
