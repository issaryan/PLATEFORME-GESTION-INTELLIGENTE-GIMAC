package cm.gimac.des.pilotage.service;

import cm.gimac.des.pilotage.domain.*; // for static metamodels
import cm.gimac.des.pilotage.domain.Risk;
import cm.gimac.des.pilotage.repository.RiskRepository;
import cm.gimac.des.pilotage.service.criteria.RiskCriteria;
import cm.gimac.des.pilotage.service.dto.RiskDTO;
import cm.gimac.des.pilotage.service.mapper.RiskMapper;
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
 * Service for executing complex queries for {@link Risk} entities in the database.
 * The main input is a {@link RiskCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link RiskDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class RiskQueryService extends QueryService<Risk> {

    private static final Logger LOG = LoggerFactory.getLogger(RiskQueryService.class);

    private final RiskRepository riskRepository;

    private final RiskMapper riskMapper;

    public RiskQueryService(RiskRepository riskRepository, RiskMapper riskMapper) {
        this.riskRepository = riskRepository;
        this.riskMapper = riskMapper;
    }

    /**
     * Return a {@link Page} of {@link RiskDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<RiskDTO> findByCriteria(RiskCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Risk> specification = createSpecification(criteria);
        return riskRepository.findAll(specification, page).map(riskMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(RiskCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Risk> specification = createSpecification(criteria);
        return riskRepository.count(specification);
    }

    /**
     * Function to convert {@link RiskCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Risk> createSpecification(RiskCriteria criteria) {
        Specification<Risk> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), Risk_.id),
                buildStringSpecification(criteria.getScope(), Risk_.scope),
                buildStringSpecification(criteria.getDescription(), Risk_.description),
                buildRangeSpecification(criteria.getProbability(), Risk_.probability),
                buildRangeSpecification(criteria.getImpact(), Risk_.impact),
                buildRangeSpecification(criteria.getControlCoefficient(), Risk_.controlCoefficient),
                buildRangeSpecification(criteria.getGrossScore(), Risk_.grossScore),
                buildRangeSpecification(criteria.getResidualScore(), Risk_.residualScore),
                buildSpecification(criteria.getZone(), Risk_.zone),
                buildSpecification(criteria.getOwnerId(), Risk_.ownerId),
                buildStringSpecification(criteria.getMitigationPlan(), Risk_.mitigationPlan),
                buildRangeSpecification(criteria.getReviewedAt(), Risk_.reviewedAt),
                buildSpecification(criteria.getProjectId(), root -> root.join(Risk_.project, JoinType.LEFT).get(Project_.id))
            );
        }
        return specification;
    }
}
