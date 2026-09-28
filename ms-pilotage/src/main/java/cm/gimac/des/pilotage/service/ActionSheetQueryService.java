package cm.gimac.des.pilotage.service;

import cm.gimac.des.pilotage.domain.*; // for static metamodels
import cm.gimac.des.pilotage.domain.ActionSheet;
import cm.gimac.des.pilotage.repository.ActionSheetRepository;
import cm.gimac.des.pilotage.service.criteria.ActionSheetCriteria;
import cm.gimac.des.pilotage.service.dto.ActionSheetDTO;
import cm.gimac.des.pilotage.service.mapper.ActionSheetMapper;
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
 * Service for executing complex queries for {@link ActionSheet} entities in the database.
 * The main input is a {@link ActionSheetCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link ActionSheetDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class ActionSheetQueryService extends QueryService<ActionSheet> {

    private static final Logger LOG = LoggerFactory.getLogger(ActionSheetQueryService.class);

    private final ActionSheetRepository actionSheetRepository;

    private final ActionSheetMapper actionSheetMapper;

    public ActionSheetQueryService(ActionSheetRepository actionSheetRepository, ActionSheetMapper actionSheetMapper) {
        this.actionSheetRepository = actionSheetRepository;
        this.actionSheetMapper = actionSheetMapper;
    }

    /**
     * Return a {@link Page} of {@link ActionSheetDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<ActionSheetDTO> findByCriteria(ActionSheetCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<ActionSheet> specification = createSpecification(criteria);
        return actionSheetRepository.findAll(specification, page).map(actionSheetMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(ActionSheetCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<ActionSheet> specification = createSpecification(criteria);
        return actionSheetRepository.count(specification);
    }

    /**
     * Function to convert {@link ActionSheetCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<ActionSheet> createSpecification(ActionSheetCriteria criteria) {
        Specification<ActionSheet> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), ActionSheet_.id),
                buildSpecification(criteria.getOriginKind(), ActionSheet_.originKind),
                buildStringSpecification(criteria.getOriginReference(), ActionSheet_.originReference),
                buildStringSpecification(criteria.getOriginService(), ActionSheet_.originService),
                buildSpecification(criteria.getPhase(), ActionSheet_.phase),
                buildStringSpecification(criteria.getObjective(), ActionSheet_.objective),
                buildStringSpecification(criteria.getCauseAnalysis(), ActionSheet_.causeAnalysis),
                buildRangeSpecification(criteria.getTargetScore(), ActionSheet_.targetScore),
                buildSpecification(criteria.getOwnerId(), ActionSheet_.ownerId),
                buildRangeSpecification(criteria.getDueDate(), ActionSheet_.dueDate),
                buildRangeSpecification(criteria.getObservationEndDate(), ActionSheet_.observationEndDate),
                buildStringSpecification(criteria.getResult(), ActionSheet_.result),
                buildRangeSpecification(criteria.getCycle(), ActionSheet_.cycle),
                buildSpecification(criteria.getProcedureDocumentRef(), ActionSheet_.procedureDocumentRef),
                buildRangeSpecification(criteria.getClosedAt(), ActionSheet_.closedAt),
                buildSpecification(criteria.getRiskId(), root -> root.join(ActionSheet_.risk, JoinType.LEFT).get(Risk_.id))
            );
        }
        return specification;
    }
}
