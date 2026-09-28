package cm.gimac.des.monitoring.service;

import cm.gimac.des.monitoring.domain.*; // for static metamodels
import cm.gimac.des.monitoring.domain.OperatorUnavailability;
import cm.gimac.des.monitoring.repository.OperatorUnavailabilityRepository;
import cm.gimac.des.monitoring.service.criteria.OperatorUnavailabilityCriteria;
import cm.gimac.des.monitoring.service.dto.OperatorUnavailabilityDTO;
import cm.gimac.des.monitoring.service.mapper.OperatorUnavailabilityMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Service for executing complex queries for {@link OperatorUnavailability} entities in the database.
 * The main input is a {@link OperatorUnavailabilityCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link OperatorUnavailabilityDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class OperatorUnavailabilityQueryService extends QueryService<OperatorUnavailability> {

    private static final Logger LOG = LoggerFactory.getLogger(OperatorUnavailabilityQueryService.class);

    private final OperatorUnavailabilityRepository operatorUnavailabilityRepository;

    private final OperatorUnavailabilityMapper operatorUnavailabilityMapper;

    public OperatorUnavailabilityQueryService(
        OperatorUnavailabilityRepository operatorUnavailabilityRepository,
        OperatorUnavailabilityMapper operatorUnavailabilityMapper
    ) {
        this.operatorUnavailabilityRepository = operatorUnavailabilityRepository;
        this.operatorUnavailabilityMapper = operatorUnavailabilityMapper;
    }

    /**
     * Return a {@link Page} of {@link OperatorUnavailabilityDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<OperatorUnavailabilityDTO> findByCriteria(OperatorUnavailabilityCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<OperatorUnavailability> specification = createSpecification(criteria);
        return operatorUnavailabilityRepository.findAll(specification, page).map(operatorUnavailabilityMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(OperatorUnavailabilityCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<OperatorUnavailability> specification = createSpecification(criteria);
        return operatorUnavailabilityRepository.count(specification);
    }

    /**
     * Function to convert {@link OperatorUnavailabilityCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<OperatorUnavailability> createSpecification(OperatorUnavailabilityCriteria criteria) {
        Specification<OperatorUnavailability> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), OperatorUnavailability_.id),
                buildSpecification(criteria.getAvailabilityId(), OperatorUnavailability_.availabilityId),
                buildSpecification(criteria.getOperatorId(), OperatorUnavailability_.operatorId),
                buildRangeSpecification(criteria.getStartsAt(), OperatorUnavailability_.startsAt),
                buildRangeSpecification(criteria.getEndsAt(), OperatorUnavailability_.endsAt),
                buildRangeSpecification(criteria.getValidatedAt(), OperatorUnavailability_.validatedAt),
                buildSpecification(criteria.getCancelled(), OperatorUnavailability_.cancelled)
            );
        }
        return specification;
    }
}
