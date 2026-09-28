package cm.gimac.des.participant.service;

import cm.gimac.des.participant.domain.*; // for static metamodels
import cm.gimac.des.participant.domain.Convention;
import cm.gimac.des.participant.repository.ConventionRepository;
import cm.gimac.des.participant.service.criteria.ConventionCriteria;
import cm.gimac.des.participant.service.dto.ConventionDTO;
import cm.gimac.des.participant.service.mapper.ConventionMapper;
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
 * Service for executing complex queries for {@link Convention} entities in the database.
 * The main input is a {@link ConventionCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link ConventionDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class ConventionQueryService extends QueryService<Convention> {

    private static final Logger LOG = LoggerFactory.getLogger(ConventionQueryService.class);

    private final ConventionRepository conventionRepository;

    private final ConventionMapper conventionMapper;

    public ConventionQueryService(ConventionRepository conventionRepository, ConventionMapper conventionMapper) {
        this.conventionRepository = conventionRepository;
        this.conventionMapper = conventionMapper;
    }

    /**
     * Return a {@link Page} of {@link ConventionDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<ConventionDTO> findByCriteria(ConventionCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Convention> specification = createSpecification(criteria);
        return conventionRepository.findAll(specification, page).map(conventionMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(ConventionCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Convention> specification = createSpecification(criteria);
        return conventionRepository.count(specification);
    }

    /**
     * Function to convert {@link ConventionCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Convention> createSpecification(ConventionCriteria criteria) {
        Specification<Convention> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), Convention_.id),
                buildSpecification(criteria.getType(), Convention_.type),
                buildStringSpecification(criteria.getReference(), Convention_.reference),
                buildRangeSpecification(criteria.getSignedOn(), Convention_.signedOn),
                buildRangeSpecification(criteria.getExpiresOn(), Convention_.expiresOn),
                buildSpecification(criteria.getDocumentId(), Convention_.documentId),
                buildStringSpecification(criteria.getDocumentSha256(), Convention_.documentSha256),
                buildSpecification(criteria.getInstitutionId(), root ->
                    root.join(Convention_.institution, JoinType.LEFT).get(Institution_.id)
                )
            );
        }
        return specification;
    }
}
