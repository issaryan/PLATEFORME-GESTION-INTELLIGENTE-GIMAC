package cm.gimac.des.pilotage.service;

import cm.gimac.des.pilotage.domain.*; // for static metamodels
import cm.gimac.des.pilotage.domain.CollaboratorProjection;
import cm.gimac.des.pilotage.repository.CollaboratorProjectionRepository;
import cm.gimac.des.pilotage.service.criteria.CollaboratorProjectionCriteria;
import cm.gimac.des.pilotage.service.dto.CollaboratorProjectionDTO;
import cm.gimac.des.pilotage.service.mapper.CollaboratorProjectionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Service for executing complex queries for {@link CollaboratorProjection} entities in the database.
 * The main input is a {@link CollaboratorProjectionCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link CollaboratorProjectionDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class CollaboratorProjectionQueryService extends QueryService<CollaboratorProjection> {

    private static final Logger LOG = LoggerFactory.getLogger(CollaboratorProjectionQueryService.class);

    private final CollaboratorProjectionRepository collaboratorProjectionRepository;

    private final CollaboratorProjectionMapper collaboratorProjectionMapper;

    public CollaboratorProjectionQueryService(
        CollaboratorProjectionRepository collaboratorProjectionRepository,
        CollaboratorProjectionMapper collaboratorProjectionMapper
    ) {
        this.collaboratorProjectionRepository = collaboratorProjectionRepository;
        this.collaboratorProjectionMapper = collaboratorProjectionMapper;
    }

    /**
     * Return a {@link Page} of {@link CollaboratorProjectionDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<CollaboratorProjectionDTO> findByCriteria(CollaboratorProjectionCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<CollaboratorProjection> specification = createSpecification(criteria);
        return collaboratorProjectionRepository.findAll(specification, page).map(collaboratorProjectionMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(CollaboratorProjectionCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<CollaboratorProjection> specification = createSpecification(criteria);
        return collaboratorProjectionRepository.count(specification);
    }

    /**
     * Function to convert {@link CollaboratorProjectionCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<CollaboratorProjection> createSpecification(CollaboratorProjectionCriteria criteria) {
        Specification<CollaboratorProjection> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), CollaboratorProjection_.id),
                buildSpecification(criteria.getCollaboratorId(), CollaboratorProjection_.collaboratorId),
                buildStringSpecification(criteria.getDisplayName(), CollaboratorProjection_.displayName),
                buildSpecification(criteria.getActive(), CollaboratorProjection_.active),
                buildStringSpecification(criteria.getServiceCode(), CollaboratorProjection_.serviceCode),
                buildRangeSpecification(criteria.getUpdatedAt(), CollaboratorProjection_.updatedAt)
            );
        }
        return specification;
    }
}
