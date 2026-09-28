package cm.gimac.des.pilotage.service;

import cm.gimac.des.pilotage.domain.*; // for static metamodels
import cm.gimac.des.pilotage.domain.AbsenceProjection;
import cm.gimac.des.pilotage.repository.AbsenceProjectionRepository;
import cm.gimac.des.pilotage.service.criteria.AbsenceProjectionCriteria;
import cm.gimac.des.pilotage.service.dto.AbsenceProjectionDTO;
import cm.gimac.des.pilotage.service.mapper.AbsenceProjectionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Service for executing complex queries for {@link AbsenceProjection} entities in the database.
 * The main input is a {@link AbsenceProjectionCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link AbsenceProjectionDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class AbsenceProjectionQueryService extends QueryService<AbsenceProjection> {

    private static final Logger LOG = LoggerFactory.getLogger(AbsenceProjectionQueryService.class);

    private final AbsenceProjectionRepository absenceProjectionRepository;

    private final AbsenceProjectionMapper absenceProjectionMapper;

    public AbsenceProjectionQueryService(
        AbsenceProjectionRepository absenceProjectionRepository,
        AbsenceProjectionMapper absenceProjectionMapper
    ) {
        this.absenceProjectionRepository = absenceProjectionRepository;
        this.absenceProjectionMapper = absenceProjectionMapper;
    }

    /**
     * Return a {@link Page} of {@link AbsenceProjectionDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<AbsenceProjectionDTO> findByCriteria(AbsenceProjectionCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<AbsenceProjection> specification = createSpecification(criteria);
        return absenceProjectionRepository.findAll(specification, page).map(absenceProjectionMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(AbsenceProjectionCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<AbsenceProjection> specification = createSpecification(criteria);
        return absenceProjectionRepository.count(specification);
    }

    /**
     * Function to convert {@link AbsenceProjectionCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<AbsenceProjection> createSpecification(AbsenceProjectionCriteria criteria) {
        Specification<AbsenceProjection> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), AbsenceProjection_.id),
                buildSpecification(criteria.getAvailabilityId(), AbsenceProjection_.availabilityId),
                buildSpecification(criteria.getCollaboratorId(), AbsenceProjection_.collaboratorId),
                buildRangeSpecification(criteria.getStartsAt(), AbsenceProjection_.startsAt),
                buildRangeSpecification(criteria.getEndsAt(), AbsenceProjection_.endsAt),
                buildRangeSpecification(criteria.getUpdatedAt(), AbsenceProjection_.updatedAt)
            );
        }
        return specification;
    }
}
