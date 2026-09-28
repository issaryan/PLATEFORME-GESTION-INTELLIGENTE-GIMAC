package cm.gimac.des.iam.service;

import cm.gimac.des.iam.domain.*; // for static metamodels
import cm.gimac.des.iam.domain.Collaborator;
import cm.gimac.des.iam.repository.CollaboratorRepository;
import cm.gimac.des.iam.service.criteria.CollaboratorCriteria;
import cm.gimac.des.iam.service.dto.CollaboratorDTO;
import cm.gimac.des.iam.service.mapper.CollaboratorMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Service for executing complex queries for {@link Collaborator} entities in the database.
 * The main input is a {@link CollaboratorCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link CollaboratorDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class CollaboratorQueryService extends QueryService<Collaborator> {

    private static final Logger LOG = LoggerFactory.getLogger(CollaboratorQueryService.class);

    private final CollaboratorRepository collaboratorRepository;

    private final CollaboratorMapper collaboratorMapper;

    public CollaboratorQueryService(CollaboratorRepository collaboratorRepository, CollaboratorMapper collaboratorMapper) {
        this.collaboratorRepository = collaboratorRepository;
        this.collaboratorMapper = collaboratorMapper;
    }

    /**
     * Return a {@link Page} of {@link CollaboratorDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<CollaboratorDTO> findByCriteria(CollaboratorCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Collaborator> specification = createSpecification(criteria);
        return collaboratorRepository.findAll(specification, page).map(collaboratorMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(CollaboratorCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Collaborator> specification = createSpecification(criteria);
        return collaboratorRepository.count(specification);
    }

    /**
     * Function to convert {@link CollaboratorCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Collaborator> createSpecification(CollaboratorCriteria criteria) {
        Specification<Collaborator> specification = Specification.unrestricted();
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = specification.and(
                Specification.allOf(
                    Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : Specification.unrestricted(),
                    buildSpecification(criteria.getId(), Collaborator_.id),
                    buildStringSpecification(criteria.getFederatedId(), Collaborator_.federatedId),
                    buildStringSpecification(criteria.getDirectoryId(), Collaborator_.directoryId),
                    buildStringSpecification(criteria.getDisplayName(), Collaborator_.displayName),
                    buildStringSpecification(criteria.getEmail(), Collaborator_.email),
                    buildSpecification(criteria.getStatus(), Collaborator_.status),
                    buildSpecification(criteria.getLevel(), Collaborator_.level),
                    buildSpecification(criteria.getInstitutionId(), Collaborator_.institutionId),
                    buildStringSpecification(criteria.getLanguage(), Collaborator_.language),
                    buildRangeSpecification(criteria.getFailedAttempts(), Collaborator_.failedAttempts),
                    buildRangeSpecification(criteria.getLastLoginAt(), Collaborator_.lastLoginAt),
                    buildRangeSpecification(criteria.getDirectoryCursor(), Collaborator_.directoryCursor)
                )
            );
        }
        return specification;
    }
}
