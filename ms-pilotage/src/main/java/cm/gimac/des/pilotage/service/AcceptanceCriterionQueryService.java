package cm.gimac.des.pilotage.service;

import cm.gimac.des.pilotage.domain.*; // for static metamodels
import cm.gimac.des.pilotage.domain.AcceptanceCriterion;
import cm.gimac.des.pilotage.repository.AcceptanceCriterionRepository;
import cm.gimac.des.pilotage.service.criteria.AcceptanceCriterionCriteria;
import cm.gimac.des.pilotage.service.dto.AcceptanceCriterionDTO;
import cm.gimac.des.pilotage.service.mapper.AcceptanceCriterionMapper;
import jakarta.persistence.criteria.JoinType;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Service for executing complex queries for {@link AcceptanceCriterion} entities in the database.
 * The main input is a {@link AcceptanceCriterionCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link List} of {@link AcceptanceCriterionDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class AcceptanceCriterionQueryService extends QueryService<AcceptanceCriterion> {

    private static final Logger LOG = LoggerFactory.getLogger(AcceptanceCriterionQueryService.class);

    private final AcceptanceCriterionRepository acceptanceCriterionRepository;

    private final AcceptanceCriterionMapper acceptanceCriterionMapper;

    public AcceptanceCriterionQueryService(
        AcceptanceCriterionRepository acceptanceCriterionRepository,
        AcceptanceCriterionMapper acceptanceCriterionMapper
    ) {
        this.acceptanceCriterionRepository = acceptanceCriterionRepository;
        this.acceptanceCriterionMapper = acceptanceCriterionMapper;
    }

    /**
     * Return a {@link List} of {@link AcceptanceCriterionDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public List<AcceptanceCriterionDTO> findByCriteria(AcceptanceCriterionCriteria criteria) {
        LOG.debug("find by criteria : {}", criteria);
        final Specification<AcceptanceCriterion> specification = createSpecification(criteria);
        return acceptanceCriterionMapper.toDto(acceptanceCriterionRepository.findAll(specification));
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(AcceptanceCriterionCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<AcceptanceCriterion> specification = createSpecification(criteria);
        return acceptanceCriterionRepository.count(specification);
    }

    /**
     * Function to convert {@link AcceptanceCriterionCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<AcceptanceCriterion> createSpecification(AcceptanceCriterionCriteria criteria) {
        Specification<AcceptanceCriterion> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), AcceptanceCriterion_.id),
                buildStringSpecification(criteria.getLabel(), AcceptanceCriterion_.label),
                buildSpecification(criteria.getSatisfied(), AcceptanceCriterion_.satisfied),
                buildSpecification(criteria.getEvidenceRequired(), AcceptanceCriterion_.evidenceRequired),
                buildSpecification(criteria.getEvidenceRef(), AcceptanceCriterion_.evidenceRef),
                buildRangeSpecification(criteria.getSatisfiedAt(), AcceptanceCriterion_.satisfiedAt),
                buildSpecification(criteria.getTaskId(), root -> root.join(AcceptanceCriterion_.task, JoinType.LEFT).get(Task_.id))
            );
        }
        return specification;
    }
}
