package cm.gimac.des.reporting.service;

import cm.gimac.des.reporting.domain.*; // for static metamodels
import cm.gimac.des.reporting.domain.IndicatorValue;
import cm.gimac.des.reporting.repository.IndicatorValueRepository;
import cm.gimac.des.reporting.service.criteria.IndicatorValueCriteria;
import cm.gimac.des.reporting.service.dto.IndicatorValueDTO;
import cm.gimac.des.reporting.service.mapper.IndicatorValueMapper;
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
 * Service for executing complex queries for {@link IndicatorValue} entities in the database.
 * The main input is a {@link IndicatorValueCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link IndicatorValueDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class IndicatorValueQueryService extends QueryService<IndicatorValue> {

    private static final Logger LOG = LoggerFactory.getLogger(IndicatorValueQueryService.class);

    private final IndicatorValueRepository indicatorValueRepository;

    private final IndicatorValueMapper indicatorValueMapper;

    public IndicatorValueQueryService(IndicatorValueRepository indicatorValueRepository, IndicatorValueMapper indicatorValueMapper) {
        this.indicatorValueRepository = indicatorValueRepository;
        this.indicatorValueMapper = indicatorValueMapper;
    }

    /**
     * Return a {@link Page} of {@link IndicatorValueDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<IndicatorValueDTO> findByCriteria(IndicatorValueCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<IndicatorValue> specification = createSpecification(criteria);
        return indicatorValueRepository.findAll(specification, page).map(indicatorValueMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(IndicatorValueCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<IndicatorValue> specification = createSpecification(criteria);
        return indicatorValueRepository.count(specification);
    }

    /**
     * Function to convert {@link IndicatorValueCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<IndicatorValue> createSpecification(IndicatorValueCriteria criteria) {
        Specification<IndicatorValue> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), IndicatorValue_.id),
                buildStringSpecification(criteria.getPeriod(), IndicatorValue_.period),
                buildRangeSpecification(criteria.getTimeKey(), IndicatorValue_.timeKey),
                buildSpecification(criteria.getInstitutionId(), IndicatorValue_.institutionId),
                buildStringSpecification(criteria.getServiceCode(), IndicatorValue_.serviceCode),
                buildStringSpecification(criteria.getSeverity(), IndicatorValue_.severity),
                buildRangeSpecification(criteria.getValue(), IndicatorValue_.value),
                buildSpecification(criteria.getNoData(), IndicatorValue_.noData),
                buildSpecification(criteria.getThresholdState(), IndicatorValue_.thresholdState),
                buildSpecification(criteria.getPreviousThresholdState(), IndicatorValue_.previousThresholdState),
                buildSpecification(criteria.getTrend(), IndicatorValue_.trend),
                buildRangeSpecification(criteria.getComputedAt(), IndicatorValue_.computedAt),
                buildSpecification(criteria.getDefinitionId(), root ->
                    root.join(IndicatorValue_.definition, JoinType.LEFT).get(IndicatorDefinition_.id)
                ),
                buildSpecification(criteria.getStatementId(), root ->
                    root.join(IndicatorValue_.statement, JoinType.LEFT).get(ImportedStatement_.id)
                )
            );
        }
        return specification;
    }
}
