package cm.gimac.des.reporting.service;

import cm.gimac.des.reporting.domain.*; // for static metamodels
import cm.gimac.des.reporting.domain.IndicatorDefinition;
import cm.gimac.des.reporting.repository.IndicatorDefinitionRepository;
import cm.gimac.des.reporting.service.criteria.IndicatorDefinitionCriteria;
import cm.gimac.des.reporting.service.dto.IndicatorDefinitionDTO;
import cm.gimac.des.reporting.service.mapper.IndicatorDefinitionMapper;
import jakarta.persistence.criteria.JoinType;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Service for executing complex queries for {@link IndicatorDefinition} entities in the database.
 * The main input is a {@link IndicatorDefinitionCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link List} of {@link IndicatorDefinitionDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class IndicatorDefinitionQueryService extends QueryService<IndicatorDefinition> {

    private static final Logger LOG = LoggerFactory.getLogger(IndicatorDefinitionQueryService.class);

    private final IndicatorDefinitionRepository indicatorDefinitionRepository;

    private final IndicatorDefinitionMapper indicatorDefinitionMapper;

    public IndicatorDefinitionQueryService(
        IndicatorDefinitionRepository indicatorDefinitionRepository,
        IndicatorDefinitionMapper indicatorDefinitionMapper
    ) {
        this.indicatorDefinitionRepository = indicatorDefinitionRepository;
        this.indicatorDefinitionMapper = indicatorDefinitionMapper;
    }

    /**
     * Return a {@link List} of {@link IndicatorDefinitionDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public List<IndicatorDefinitionDTO> findByCriteria(IndicatorDefinitionCriteria criteria) {
        LOG.debug("find by criteria : {}", criteria);
        final Specification<IndicatorDefinition> specification = createSpecification(criteria);
        return indicatorDefinitionMapper.toDto(indicatorDefinitionRepository.findAll(specification));
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(IndicatorDefinitionCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<IndicatorDefinition> specification = createSpecification(criteria);
        return indicatorDefinitionRepository.count(specification);
    }

    /**
     * Function to convert {@link IndicatorDefinitionCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<IndicatorDefinition> createSpecification(IndicatorDefinitionCriteria criteria) {
        Specification<IndicatorDefinition> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), IndicatorDefinition_.id),
                buildStringSpecification(criteria.getCode(), IndicatorDefinition_.code),
                buildStringSpecification(criteria.getLabel(), IndicatorDefinition_.label),
                buildSpecification(criteria.getSource(), IndicatorDefinition_.source),
                buildStringSpecification(criteria.getUnit(), IndicatorDefinition_.unit),
                buildRangeSpecification(criteria.getWindow(), IndicatorDefinition_.window),
                buildRangeSpecification(criteria.getSegmentCount(), IndicatorDefinition_.segmentCount),
                buildRangeSpecification(criteria.getSegmentLength(), IndicatorDefinition_.segmentLength),
                buildRangeSpecification(criteria.getWarningThreshold(), IndicatorDefinition_.warningThreshold),
                buildRangeSpecification(criteria.getCriticalThreshold(), IndicatorDefinition_.criticalThreshold),
                buildSpecification(criteria.getHigherIsWorse(), IndicatorDefinition_.higherIsWorse),
                buildStringSpecification(criteria.getAudience(), IndicatorDefinition_.audience),
                buildSpecification(criteria.getActive(), IndicatorDefinition_.active),
                buildSpecification(criteria.getCockpitsId(), root ->
                    root.join(IndicatorDefinition_.cockpits, JoinType.LEFT).get(CockpitView_.id)
                )
            );
        }
        return specification;
    }
}
