package cm.gimac.des.iam.service;

import cm.gimac.des.iam.domain.*; // for static metamodels
import cm.gimac.des.iam.domain.UserSession;
import cm.gimac.des.iam.repository.UserSessionRepository;
import cm.gimac.des.iam.service.criteria.UserSessionCriteria;
import cm.gimac.des.iam.service.dto.UserSessionDTO;
import cm.gimac.des.iam.service.mapper.UserSessionMapper;
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
 * Service for executing complex queries for {@link UserSession} entities in the database.
 * The main input is a {@link UserSessionCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link UserSessionDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class UserSessionQueryService extends QueryService<UserSession> {

    private static final Logger LOG = LoggerFactory.getLogger(UserSessionQueryService.class);

    private final UserSessionRepository userSessionRepository;

    private final UserSessionMapper userSessionMapper;

    public UserSessionQueryService(UserSessionRepository userSessionRepository, UserSessionMapper userSessionMapper) {
        this.userSessionRepository = userSessionRepository;
        this.userSessionMapper = userSessionMapper;
    }

    /**
     * Return a {@link Page} of {@link UserSessionDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<UserSessionDTO> findByCriteria(UserSessionCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<UserSession> specification = createSpecification(criteria);
        return userSessionRepository.findAll(specification, page).map(userSessionMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(UserSessionCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<UserSession> specification = createSpecification(criteria);
        return userSessionRepository.count(specification);
    }

    /**
     * Function to convert {@link UserSessionCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<UserSession> createSpecification(UserSessionCriteria criteria) {
        Specification<UserSession> specification = Specification.unrestricted();
        specification = specification.and((root, query, builder) -> {
            if (Long.class != query.getResultType()) {
                root.fetch(UserSession_.collaborator, JoinType.LEFT);
            }
            return null;
        });
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = specification.and(
                Specification.allOf(
                    Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : Specification.unrestricted(),
                    buildSpecification(criteria.getId(), UserSession_.id),
                    buildStringSpecification(criteria.getCertificateThumbprint(), UserSession_.certificateThumbprint),
                    buildStringSpecification(criteria.getAuthMethods(), UserSession_.authMethods),
                    buildRangeSpecification(criteria.getIssuedAt(), UserSession_.issuedAt),
                    buildRangeSpecification(criteria.getExpiresAt(), UserSession_.expiresAt),
                    buildSpecification(criteria.getState(), UserSession_.state),
                    buildRangeSpecification(criteria.getRevokedAt(), UserSession_.revokedAt),
                    buildStringSpecification(criteria.getRevocationReason(), UserSession_.revocationReason),
                    buildSpecification(criteria.getCollaboratorId(), root ->
                        root.join(UserSession_.collaborator, JoinType.LEFT).get(Collaborator_.id)
                    )
                )
            );
        }
        return specification;
    }
}
