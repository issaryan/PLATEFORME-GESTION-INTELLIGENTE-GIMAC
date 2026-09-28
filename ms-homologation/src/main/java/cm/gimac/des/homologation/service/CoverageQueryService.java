package cm.gimac.des.homologation.service;

import cm.gimac.des.homologation.domain.*; // for static metamodels
import cm.gimac.des.homologation.domain.Coverage;
import cm.gimac.des.homologation.repository.CoverageRepository;
import cm.gimac.des.homologation.service.criteria.CoverageCriteria;
import cm.gimac.des.homologation.service.dto.CoverageDTO;
import cm.gimac.des.homologation.service.mapper.CoverageMapper;
import jakarta.persistence.criteria.JoinType;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Service for executing complex queries for {@link Coverage} entities in the database.
 * The main input is a {@link CoverageCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link List} of {@link CoverageDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class CoverageQueryService extends QueryService<Coverage> {

    private static final Logger LOG = LoggerFactory.getLogger(CoverageQueryService.class);

    private final CoverageRepository coverageRepository;

    private final CoverageMapper coverageMapper;

    public CoverageQueryService(CoverageRepository coverageRepository, CoverageMapper coverageMapper) {
        this.coverageRepository = coverageRepository;
        this.coverageMapper = coverageMapper;
    }

    /**
     * Return a {@link List} of {@link CoverageDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public List<CoverageDTO> findByCriteria(CoverageCriteria criteria) {
        LOG.debug("find by criteria : {}", criteria);
        final Specification<Coverage> specification = createSpecification(criteria);
        return coverageMapper.toDto(coverageRepository.findAll(specification));
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(CoverageCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Coverage> specification = createSpecification(criteria);
        return coverageRepository.count(specification);
    }

    /**
     * Function to convert {@link CoverageCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Coverage> createSpecification(CoverageCriteria criteria) {
        Specification<Coverage> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), Coverage_.id),
                buildStringSpecification(criteria.getServiceCode(), Coverage_.serviceCode),
                buildRangeSpecification(criteria.getCertificationRate(), Coverage_.certificationRate),
                buildRangeSpecification(criteria.getCommissioningRate(), Coverage_.commissioningRate),
                buildRangeSpecification(criteria.getRequiredCount(), Coverage_.requiredCount),
                buildRangeSpecification(criteria.getClosedCount(), Coverage_.closedCount),
                buildRangeSpecification(criteria.getComputedAt(), Coverage_.computedAt),
                buildSpecification(criteria.getLiaisonId(), root -> root.join(Coverage_.liaison, JoinType.LEFT).get(Liaison_.id))
            );
        }
        return specification;
    }
}
