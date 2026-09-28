package cm.gimac.des.iam.service;

import cm.gimac.des.iam.domain.*; // for static metamodels
import cm.gimac.des.iam.domain.RoleGrant;
import cm.gimac.des.iam.repository.RoleGrantRepository;
import cm.gimac.des.iam.service.criteria.RoleGrantCriteria;
import cm.gimac.des.iam.service.dto.RoleGrantDTO;
import cm.gimac.des.iam.service.mapper.RoleGrantMapper;
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
 * Service for executing complex queries for {@link RoleGrant} entities in the database.
 * The main input is a {@link RoleGrantCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link RoleGrantDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class RoleGrantQueryService extends QueryService<RoleGrant> {

    private static final Logger LOG = LoggerFactory.getLogger(RoleGrantQueryService.class);

    private final RoleGrantRepository roleGrantRepository;

    private final RoleGrantMapper roleGrantMapper;

    public RoleGrantQueryService(RoleGrantRepository roleGrantRepository, RoleGrantMapper roleGrantMapper) {
        this.roleGrantRepository = roleGrantRepository;
        this.roleGrantMapper = roleGrantMapper;
    }

    /**
     * Return a {@link Page} of {@link RoleGrantDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<RoleGrantDTO> findByCriteria(RoleGrantCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<RoleGrant> specification = createSpecification(criteria);
        return roleGrantRepository.findAll(specification, page).map(roleGrantMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(RoleGrantCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<RoleGrant> specification = createSpecification(criteria);
        return roleGrantRepository.count(specification);
    }

    /**
     * Function to convert {@link RoleGrantCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<RoleGrant> createSpecification(RoleGrantCriteria criteria) {
        Specification<RoleGrant> specification = Specification.unrestricted();
        specification = specification.and((root, query, builder) -> {
            if (Long.class != query.getResultType()) {
                root.fetch(RoleGrant_.collaborator, JoinType.LEFT);
                root.fetch(RoleGrant_.role, JoinType.LEFT);
            }
            return null;
        });
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = specification.and(
                Specification.allOf(
                    Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : Specification.unrestricted(),
                    buildSpecification(criteria.getId(), RoleGrant_.id),
                    buildSpecification(criteria.getState(), RoleGrant_.state),
                    buildSpecification(criteria.getRequestedBy(), RoleGrant_.requestedBy),
                    buildRangeSpecification(criteria.getRequestedAt(), RoleGrant_.requestedAt),
                    buildSpecification(criteria.getApprovedBy(), RoleGrant_.approvedBy),
                    buildRangeSpecification(criteria.getApprovedAt(), RoleGrant_.approvedAt),
                    buildRangeSpecification(criteria.getRevokedAt(), RoleGrant_.revokedAt),
                    buildStringSpecification(criteria.getRevocationReason(), RoleGrant_.revocationReason),
                    buildSpecification(criteria.getCollaboratorId(), root ->
                        root.join(RoleGrant_.collaborator, JoinType.LEFT).get(Collaborator_.id)
                    ),
                    buildSpecification(criteria.getRoleId(), root -> root.join(RoleGrant_.role, JoinType.LEFT).get(Role_.id))
                )
            );
        }
        return specification;
    }
}
