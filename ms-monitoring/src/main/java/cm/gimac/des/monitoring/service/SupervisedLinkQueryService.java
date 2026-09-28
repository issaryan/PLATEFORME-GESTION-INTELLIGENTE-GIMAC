package cm.gimac.des.monitoring.service;

import cm.gimac.des.monitoring.domain.*; // for static metamodels
import cm.gimac.des.monitoring.domain.SupervisedLink;
import cm.gimac.des.monitoring.repository.SupervisedLinkRepository;
import cm.gimac.des.monitoring.service.criteria.SupervisedLinkCriteria;
import cm.gimac.des.monitoring.service.dto.SupervisedLinkDTO;
import cm.gimac.des.monitoring.service.mapper.SupervisedLinkMapper;
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
 * Service for executing complex queries for {@link SupervisedLink} entities in the database.
 * The main input is a {@link SupervisedLinkCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link SupervisedLinkDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class SupervisedLinkQueryService extends QueryService<SupervisedLink> {

    private static final Logger LOG = LoggerFactory.getLogger(SupervisedLinkQueryService.class);

    private final SupervisedLinkRepository supervisedLinkRepository;

    private final SupervisedLinkMapper supervisedLinkMapper;

    public SupervisedLinkQueryService(SupervisedLinkRepository supervisedLinkRepository, SupervisedLinkMapper supervisedLinkMapper) {
        this.supervisedLinkRepository = supervisedLinkRepository;
        this.supervisedLinkMapper = supervisedLinkMapper;
    }

    /**
     * Return a {@link Page} of {@link SupervisedLinkDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<SupervisedLinkDTO> findByCriteria(SupervisedLinkCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<SupervisedLink> specification = createSpecification(criteria);
        return supervisedLinkRepository.findAll(specification, page).map(supervisedLinkMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(SupervisedLinkCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<SupervisedLink> specification = createSpecification(criteria);
        return supervisedLinkRepository.count(specification);
    }

    /**
     * Function to convert {@link SupervisedLinkCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<SupervisedLink> createSpecification(SupervisedLinkCriteria criteria) {
        Specification<SupervisedLink> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), SupervisedLink_.id),
                buildStringSpecification(criteria.getCode(), SupervisedLink_.code),
                buildStringSpecification(criteria.getLabel(), SupervisedLink_.label),
                buildSpecification(criteria.getScope(), SupervisedLink_.scope),
                buildSpecification(criteria.getInstitutionId(), SupervisedLink_.institutionId),
                buildStringSpecification(criteria.getType(), SupervisedLink_.type),
                buildSpecification(criteria.getState(), SupervisedLink_.state),
                buildRangeSpecification(criteria.getStateSince(), SupervisedLink_.stateSince),
                buildRangeSpecification(criteria.getLastMeasureAt(), SupervisedLink_.lastMeasureAt),
                buildRangeSpecification(criteria.getMeasurePeriod(), SupervisedLink_.measurePeriod),
                buildRangeSpecification(criteria.getMaintenanceFrom(), SupervisedLink_.maintenanceFrom),
                buildRangeSpecification(criteria.getMaintenanceUntil(), SupervisedLink_.maintenanceUntil),
                buildSpecification(criteria.getParentPlatformId(), root ->
                    root.join(SupervisedLink_.parentPlatform, JoinType.LEFT).get(SupervisedLink_.id)
                )
            );
        }
        return specification;
    }
}
