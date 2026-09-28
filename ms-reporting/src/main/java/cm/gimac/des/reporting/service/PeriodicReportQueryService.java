package cm.gimac.des.reporting.service;

import cm.gimac.des.reporting.domain.*; // for static metamodels
import cm.gimac.des.reporting.domain.PeriodicReport;
import cm.gimac.des.reporting.repository.PeriodicReportRepository;
import cm.gimac.des.reporting.service.criteria.PeriodicReportCriteria;
import cm.gimac.des.reporting.service.dto.PeriodicReportDTO;
import cm.gimac.des.reporting.service.mapper.PeriodicReportMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Service for executing complex queries for {@link PeriodicReport} entities in the database.
 * The main input is a {@link PeriodicReportCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link PeriodicReportDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class PeriodicReportQueryService extends QueryService<PeriodicReport> {

    private static final Logger LOG = LoggerFactory.getLogger(PeriodicReportQueryService.class);

    private final PeriodicReportRepository periodicReportRepository;

    private final PeriodicReportMapper periodicReportMapper;

    public PeriodicReportQueryService(PeriodicReportRepository periodicReportRepository, PeriodicReportMapper periodicReportMapper) {
        this.periodicReportRepository = periodicReportRepository;
        this.periodicReportMapper = periodicReportMapper;
    }

    /**
     * Return a {@link Page} of {@link PeriodicReportDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<PeriodicReportDTO> findByCriteria(PeriodicReportCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<PeriodicReport> specification = createSpecification(criteria);
        return periodicReportRepository.findAll(specification, page).map(periodicReportMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(PeriodicReportCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<PeriodicReport> specification = createSpecification(criteria);
        return periodicReportRepository.count(specification);
    }

    /**
     * Function to convert {@link PeriodicReportCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<PeriodicReport> createSpecification(PeriodicReportCriteria criteria) {
        Specification<PeriodicReport> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), PeriodicReport_.id),
                buildStringSpecification(criteria.getServiceCode(), PeriodicReport_.serviceCode),
                buildSpecification(criteria.getPeriodicity(), PeriodicReport_.periodicity),
                buildStringSpecification(criteria.getPeriod(), PeriodicReport_.period),
                buildRangeSpecification(criteria.getReportVersion(), PeriodicReport_.reportVersion),
                buildSpecification(criteria.getManual(), PeriodicReport_.manual),
                buildStringSpecification(criteria.getContentDigest(), PeriodicReport_.contentDigest),
                buildRangeSpecification(criteria.getGeneratedAt(), PeriodicReport_.generatedAt),
                buildSpecification(criteria.getSignerId(), PeriodicReport_.signerId),
                buildRangeSpecification(criteria.getSignedAt(), PeriodicReport_.signedAt),
                buildSpecification(criteria.getDocumentRef(), PeriodicReport_.documentRef),
                buildRangeSpecification(criteria.getNotifiedAt(), PeriodicReport_.notifiedAt),
                buildSpecification(criteria.getState(), PeriodicReport_.state)
            );
        }
        return specification;
    }
}
