package cm.gimac.des.pilotage.service;

import cm.gimac.des.pilotage.domain.*; // for static metamodels
import cm.gimac.des.pilotage.domain.Milestone;
import cm.gimac.des.pilotage.repository.MilestoneRepository;
import cm.gimac.des.pilotage.service.criteria.MilestoneCriteria;
import cm.gimac.des.pilotage.service.dto.MilestoneDTO;
import cm.gimac.des.pilotage.service.mapper.MilestoneMapper;
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
 * Service for executing complex queries for {@link Milestone} entities in the database.
 * The main input is a {@link MilestoneCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link MilestoneDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class MilestoneQueryService extends QueryService<Milestone> {

    private static final Logger LOG = LoggerFactory.getLogger(MilestoneQueryService.class);

    private final MilestoneRepository milestoneRepository;

    private final MilestoneMapper milestoneMapper;

    public MilestoneQueryService(MilestoneRepository milestoneRepository, MilestoneMapper milestoneMapper) {
        this.milestoneRepository = milestoneRepository;
        this.milestoneMapper = milestoneMapper;
    }

    /**
     * Return a {@link Page} of {@link MilestoneDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<MilestoneDTO> findByCriteria(MilestoneCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Milestone> specification = createSpecification(criteria);
        return milestoneRepository.fetchBagRelationships(milestoneRepository.findAll(specification, page)).map(milestoneMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(MilestoneCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Milestone> specification = createSpecification(criteria);
        return milestoneRepository.count(specification);
    }

    /**
     * Function to convert {@link MilestoneCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Milestone> createSpecification(MilestoneCriteria criteria) {
        Specification<Milestone> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), Milestone_.id),
                buildStringSpecification(criteria.getLabel(), Milestone_.label),
                buildRangeSpecification(criteria.getOptimistic(), Milestone_.optimistic),
                buildRangeSpecification(criteria.getMostLikely(), Milestone_.mostLikely),
                buildRangeSpecification(criteria.getPessimistic(), Milestone_.pessimistic),
                buildRangeSpecification(criteria.getExpectedDuration(), Milestone_.expectedDuration),
                buildRangeSpecification(criteria.getVariance(), Milestone_.variance),
                buildRangeSpecification(criteria.getEarliestStart(), Milestone_.earliestStart),
                buildRangeSpecification(criteria.getLatestStart(), Milestone_.latestStart),
                buildRangeSpecification(criteria.getTotalFloat(), Milestone_.totalFloat),
                buildRangeSpecification(criteria.getFreeFloat(), Milestone_.freeFloat),
                buildSpecification(criteria.getCritical(), Milestone_.critical),
                buildSpecification(criteria.getState(), Milestone_.state),
                buildRangeSpecification(criteria.getActualDuration(), Milestone_.actualDuration),
                buildRangeSpecification(criteria.getReachedAt(), Milestone_.reachedAt),
                buildSpecification(criteria.getProjectId(), root -> root.join(Milestone_.project, JoinType.LEFT).get(Project_.id)),
                buildSpecification(criteria.getPredecessorsId(), root ->
                    root.join(Milestone_.predecessors, JoinType.LEFT).get(Milestone_.id)
                ),
                buildSpecification(criteria.getSuccessorsId(), root -> root.join(Milestone_.successors, JoinType.LEFT).get(Milestone_.id))
            );
        }
        return specification;
    }
}
