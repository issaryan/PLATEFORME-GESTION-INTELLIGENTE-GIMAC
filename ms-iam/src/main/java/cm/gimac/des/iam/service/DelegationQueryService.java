package cm.gimac.des.iam.service;

import cm.gimac.des.iam.domain.*; // for static metamodels
import cm.gimac.des.iam.domain.Delegation;
import cm.gimac.des.iam.repository.DelegationRepository;
import cm.gimac.des.iam.service.criteria.DelegationCriteria;
import cm.gimac.des.iam.service.dto.DelegationDTO;
import cm.gimac.des.iam.service.mapper.DelegationMapper;
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
 * Service for executing complex queries for {@link Delegation} entities in the database.
 * The main input is a {@link DelegationCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link DelegationDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class DelegationQueryService extends QueryService<Delegation> {

    private static final Logger LOG = LoggerFactory.getLogger(DelegationQueryService.class);

    private final DelegationRepository delegationRepository;

    private final DelegationMapper delegationMapper;

    public DelegationQueryService(DelegationRepository delegationRepository, DelegationMapper delegationMapper) {
        this.delegationRepository = delegationRepository;
        this.delegationMapper = delegationMapper;
    }

    /**
     * Return a {@link Page} of {@link DelegationDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<DelegationDTO> findByCriteria(DelegationCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Delegation> specification = createSpecification(criteria);
        return delegationRepository.findAll(specification, page).map(delegationMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(DelegationCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Delegation> specification = createSpecification(criteria);
        return delegationRepository.count(specification);
    }

    /**
     * Function to convert {@link DelegationCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Delegation> createSpecification(DelegationCriteria criteria) {
        Specification<Delegation> specification = Specification.unrestricted();
        specification = specification.and((root, query, builder) -> {
            if (Long.class != query.getResultType()) {
                root.fetch(Delegation_.collaborator, JoinType.LEFT);
            }
            return null;
        });
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = specification.and(
                Specification.allOf(
                    Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : Specification.unrestricted(),
                    buildSpecification(criteria.getId(), Delegation_.id),
                    buildSpecification(criteria.getType(), Delegation_.type),
                    buildStringSpecification(criteria.getReason(), Delegation_.reason),
                    buildSpecification(criteria.getDelegatedLevel(), Delegation_.delegatedLevel),
                    buildSpecification(criteria.getRequesterId(), Delegation_.requesterId),
                    buildSpecification(criteria.getApproverId(), Delegation_.approverId),
                    buildRangeSpecification(criteria.getStartsAt(), Delegation_.startsAt),
                    buildRangeSpecification(criteria.getEndsAt(), Delegation_.endsAt),
                    buildSpecification(criteria.getState(), Delegation_.state),
                    buildSpecification(criteria.getCollaboratorId(), root ->
                        root.join(Delegation_.collaborator, JoinType.LEFT).get(Collaborator_.id)
                    )
                )
            );
        }
        return specification;
    }
}
