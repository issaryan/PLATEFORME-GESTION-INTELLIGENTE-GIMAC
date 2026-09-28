package cm.gimac.des.iam.service;

import cm.gimac.des.iam.domain.*; // for static metamodels
import cm.gimac.des.iam.domain.Availability;
import cm.gimac.des.iam.repository.AvailabilityRepository;
import cm.gimac.des.iam.service.criteria.AvailabilityCriteria;
import cm.gimac.des.iam.service.dto.AvailabilityDTO;
import cm.gimac.des.iam.service.mapper.AvailabilityMapper;
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
 * Service for executing complex queries for {@link Availability} entities in the database.
 * The main input is a {@link AvailabilityCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link AvailabilityDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class AvailabilityQueryService extends QueryService<Availability> {

    private static final Logger LOG = LoggerFactory.getLogger(AvailabilityQueryService.class);

    private final AvailabilityRepository availabilityRepository;

    private final AvailabilityMapper availabilityMapper;

    public AvailabilityQueryService(AvailabilityRepository availabilityRepository, AvailabilityMapper availabilityMapper) {
        this.availabilityRepository = availabilityRepository;
        this.availabilityMapper = availabilityMapper;
    }

    /**
     * Return a {@link Page} of {@link AvailabilityDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<AvailabilityDTO> findByCriteria(AvailabilityCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Availability> specification = createSpecification(criteria);
        return availabilityRepository.findAll(specification, page).map(availabilityMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(AvailabilityCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Availability> specification = createSpecification(criteria);
        return availabilityRepository.count(specification);
    }

    /**
     * Function to convert {@link AvailabilityCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Availability> createSpecification(AvailabilityCriteria criteria) {
        Specification<Availability> specification = Specification.unrestricted();
        specification = specification.and((root, query, builder) -> {
            if (Long.class != query.getResultType()) {
                root.fetch(Availability_.collaborator, JoinType.LEFT);
            }
            return null;
        });
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = specification.and(
                Specification.allOf(
                    Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : Specification.unrestricted(),
                    buildSpecification(criteria.getId(), Availability_.id),
                    buildSpecification(criteria.getType(), Availability_.type),
                    buildRangeSpecification(criteria.getStartsAt(), Availability_.startsAt),
                    buildRangeSpecification(criteria.getEndsAt(), Availability_.endsAt),
                    buildStringSpecification(criteria.getReason(), Availability_.reason),
                    buildSpecification(criteria.getJustificationDocumentId(), Availability_.justificationDocumentId),
                    buildSpecification(criteria.getValidationState(), Availability_.validationState),
                    buildSpecification(criteria.getValidatorId(), Availability_.validatorId),
                    buildRangeSpecification(criteria.getValidatedAt(), Availability_.validatedAt),
                    buildSpecification(criteria.getCollaboratorId(), root ->
                        root.join(Availability_.collaborator, JoinType.LEFT).get(Collaborator_.id)
                    )
                )
            );
        }
        return specification;
    }
}
