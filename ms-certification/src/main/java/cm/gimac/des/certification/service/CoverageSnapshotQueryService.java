package cm.gimac.des.certification.service;

import cm.gimac.des.certification.domain.*; // for static metamodels
import cm.gimac.des.certification.domain.CoverageSnapshot;
import cm.gimac.des.certification.repository.CoverageSnapshotRepository;
import cm.gimac.des.certification.service.criteria.CoverageSnapshotCriteria;
import cm.gimac.des.certification.service.dto.CoverageSnapshotDTO;
import cm.gimac.des.certification.service.mapper.CoverageSnapshotMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Service for executing complex queries for {@link CoverageSnapshot} entities in the database.
 * The main input is a {@link CoverageSnapshotCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link CoverageSnapshotDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class CoverageSnapshotQueryService extends QueryService<CoverageSnapshot> {

    private static final Logger LOG = LoggerFactory.getLogger(CoverageSnapshotQueryService.class);

    private final CoverageSnapshotRepository coverageSnapshotRepository;

    private final CoverageSnapshotMapper coverageSnapshotMapper;

    public CoverageSnapshotQueryService(
        CoverageSnapshotRepository coverageSnapshotRepository,
        CoverageSnapshotMapper coverageSnapshotMapper
    ) {
        this.coverageSnapshotRepository = coverageSnapshotRepository;
        this.coverageSnapshotMapper = coverageSnapshotMapper;
    }

    /**
     * Return a {@link Page} of {@link CoverageSnapshotDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<CoverageSnapshotDTO> findByCriteria(CoverageSnapshotCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<CoverageSnapshot> specification = createSpecification(criteria);
        return coverageSnapshotRepository.findAll(specification, page).map(coverageSnapshotMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(CoverageSnapshotCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<CoverageSnapshot> specification = createSpecification(criteria);
        return coverageSnapshotRepository.count(specification);
    }

    /**
     * Function to convert {@link CoverageSnapshotCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<CoverageSnapshot> createSpecification(CoverageSnapshotCriteria criteria) {
        Specification<CoverageSnapshot> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), CoverageSnapshot_.id),
                buildSpecification(criteria.getLiaisonId(), CoverageSnapshot_.liaisonId),
                buildStringSpecification(criteria.getServiceCode(), CoverageSnapshot_.serviceCode),
                buildSpecification(criteria.getIncluded(), CoverageSnapshot_.included),
                buildRangeSpecification(criteria.getCertificationRate(), CoverageSnapshot_.certificationRate),
                buildRangeSpecification(criteria.getCommissioningRate(), CoverageSnapshot_.commissioningRate),
                buildSpecification(criteria.getSourceEventId(), CoverageSnapshot_.sourceEventId),
                buildRangeSpecification(criteria.getComputedAt(), CoverageSnapshot_.computedAt),
                buildRangeSpecification(criteria.getReceivedAt(), CoverageSnapshot_.receivedAt)
            );
        }
        return specification;
    }
}
