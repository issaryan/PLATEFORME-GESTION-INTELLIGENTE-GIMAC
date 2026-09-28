package cm.gimac.des.pilotage.service;

import cm.gimac.des.pilotage.domain.*; // for static metamodels
import cm.gimac.des.pilotage.domain.RiskAssessment;
import cm.gimac.des.pilotage.repository.RiskAssessmentRepository;
import cm.gimac.des.pilotage.service.criteria.RiskAssessmentCriteria;
import cm.gimac.des.pilotage.service.dto.RiskAssessmentDTO;
import cm.gimac.des.pilotage.service.mapper.RiskAssessmentMapper;
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
 * Service for executing complex queries for {@link RiskAssessment} entities in the database.
 * The main input is a {@link RiskAssessmentCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link RiskAssessmentDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class RiskAssessmentQueryService extends QueryService<RiskAssessment> {

    private static final Logger LOG = LoggerFactory.getLogger(RiskAssessmentQueryService.class);

    private final RiskAssessmentRepository riskAssessmentRepository;

    private final RiskAssessmentMapper riskAssessmentMapper;

    public RiskAssessmentQueryService(RiskAssessmentRepository riskAssessmentRepository, RiskAssessmentMapper riskAssessmentMapper) {
        this.riskAssessmentRepository = riskAssessmentRepository;
        this.riskAssessmentMapper = riskAssessmentMapper;
    }

    /**
     * Return a {@link Page} of {@link RiskAssessmentDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<RiskAssessmentDTO> findByCriteria(RiskAssessmentCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<RiskAssessment> specification = createSpecification(criteria);
        return riskAssessmentRepository.findAll(specification, page).map(riskAssessmentMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(RiskAssessmentCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<RiskAssessment> specification = createSpecification(criteria);
        return riskAssessmentRepository.count(specification);
    }

    /**
     * Function to convert {@link RiskAssessmentCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<RiskAssessment> createSpecification(RiskAssessmentCriteria criteria) {
        Specification<RiskAssessment> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), RiskAssessment_.id),
                buildRangeSpecification(criteria.getProbability(), RiskAssessment_.probability),
                buildRangeSpecification(criteria.getImpact(), RiskAssessment_.impact),
                buildRangeSpecification(criteria.getControlCoefficient(), RiskAssessment_.controlCoefficient),
                buildRangeSpecification(criteria.getGrossScore(), RiskAssessment_.grossScore),
                buildRangeSpecification(criteria.getResidualScore(), RiskAssessment_.residualScore),
                buildSpecification(criteria.getPreviousZone(), RiskAssessment_.previousZone),
                buildSpecification(criteria.getZone(), RiskAssessment_.zone),
                buildSpecification(criteria.getAssessorId(), RiskAssessment_.assessorId),
                buildRangeSpecification(criteria.getAssessedAt(), RiskAssessment_.assessedAt),
                buildSpecification(criteria.getRiskId(), root -> root.join(RiskAssessment_.risk, JoinType.LEFT).get(Risk_.id))
            );
        }
        return specification;
    }
}
