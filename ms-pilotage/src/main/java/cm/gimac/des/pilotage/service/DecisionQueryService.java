package cm.gimac.des.pilotage.service;

import cm.gimac.des.pilotage.domain.*; // for static metamodels
import cm.gimac.des.pilotage.domain.Decision;
import cm.gimac.des.pilotage.repository.DecisionRepository;
import cm.gimac.des.pilotage.service.criteria.DecisionCriteria;
import cm.gimac.des.pilotage.service.dto.DecisionDTO;
import cm.gimac.des.pilotage.service.mapper.DecisionMapper;
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
 * Service for executing complex queries for {@link Decision} entities in the database.
 * The main input is a {@link DecisionCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link DecisionDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class DecisionQueryService extends QueryService<Decision> {

    private static final Logger LOG = LoggerFactory.getLogger(DecisionQueryService.class);

    private final DecisionRepository decisionRepository;

    private final DecisionMapper decisionMapper;

    public DecisionQueryService(DecisionRepository decisionRepository, DecisionMapper decisionMapper) {
        this.decisionRepository = decisionRepository;
        this.decisionMapper = decisionMapper;
    }

    /**
     * Return a {@link Page} of {@link DecisionDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<DecisionDTO> findByCriteria(DecisionCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Decision> specification = createSpecification(criteria);
        return decisionRepository.findAll(specification, page).map(decisionMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(DecisionCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Decision> specification = createSpecification(criteria);
        return decisionRepository.count(specification);
    }

    /**
     * Function to convert {@link DecisionCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Decision> createSpecification(DecisionCriteria criteria) {
        Specification<Decision> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), Decision_.id),
                buildStringSpecification(criteria.getText(), Decision_.text),
                buildSpecification(criteria.getOwnerId(), Decision_.ownerId),
                buildRangeSpecification(criteria.getDueDate(), Decision_.dueDate),
                buildSpecification(criteria.getState(), Decision_.state),
                buildStringSpecification(criteria.getReturnReason(), Decision_.returnReason),
                buildRangeSpecification(criteria.getConvertedAt(), Decision_.convertedAt),
                buildSpecification(criteria.getTaskId(), root -> root.join(Decision_.task, JoinType.LEFT).get(Task_.id)),
                buildSpecification(criteria.getMeetingId(), root -> root.join(Decision_.meeting, JoinType.LEFT).get(Meeting_.id)),
                buildSpecification(criteria.getRectifiesId(), root -> root.join(Decision_.rectifies, JoinType.LEFT).get(Decision_.id))
            );
        }
        return specification;
    }
}
