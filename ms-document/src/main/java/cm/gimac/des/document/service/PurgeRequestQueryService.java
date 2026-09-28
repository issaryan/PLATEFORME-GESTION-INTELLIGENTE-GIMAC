package cm.gimac.des.document.service;

import cm.gimac.des.document.domain.*; // for static metamodels
import cm.gimac.des.document.domain.PurgeRequest;
import cm.gimac.des.document.repository.PurgeRequestRepository;
import cm.gimac.des.document.service.criteria.PurgeRequestCriteria;
import cm.gimac.des.document.service.dto.PurgeRequestDTO;
import cm.gimac.des.document.service.mapper.PurgeRequestMapper;
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
 * Service for executing complex queries for {@link PurgeRequest} entities in the database.
 * The main input is a {@link PurgeRequestCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link PurgeRequestDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class PurgeRequestQueryService extends QueryService<PurgeRequest> {

    private static final Logger LOG = LoggerFactory.getLogger(PurgeRequestQueryService.class);

    private final PurgeRequestRepository purgeRequestRepository;

    private final PurgeRequestMapper purgeRequestMapper;

    public PurgeRequestQueryService(PurgeRequestRepository purgeRequestRepository, PurgeRequestMapper purgeRequestMapper) {
        this.purgeRequestRepository = purgeRequestRepository;
        this.purgeRequestMapper = purgeRequestMapper;
    }

    /**
     * Return a {@link Page} of {@link PurgeRequestDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<PurgeRequestDTO> findByCriteria(PurgeRequestCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<PurgeRequest> specification = createSpecification(criteria);
        return purgeRequestRepository.findAll(specification, page).map(purgeRequestMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(PurgeRequestCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<PurgeRequest> specification = createSpecification(criteria);
        return purgeRequestRepository.count(specification);
    }

    /**
     * Function to convert {@link PurgeRequestCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<PurgeRequest> createSpecification(PurgeRequestCriteria criteria) {
        Specification<PurgeRequest> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), PurgeRequest_.id),
                buildSpecification(criteria.getRequesterId(), PurgeRequest_.requesterId),
                buildStringSpecification(criteria.getReason(), PurgeRequest_.reason),
                buildRangeSpecification(criteria.getRequestedAt(), PurgeRequest_.requestedAt),
                buildSpecification(criteria.getApproverId(), PurgeRequest_.approverId),
                buildRangeSpecification(criteria.getApprovedAt(), PurgeRequest_.approvedAt),
                buildSpecification(criteria.getState(), PurgeRequest_.state),
                buildStringSpecification(criteria.getRejectionReason(), PurgeRequest_.rejectionReason),
                buildRangeSpecification(criteria.getExecutedAt(), PurgeRequest_.executedAt),
                buildSpecification(criteria.getDocumentId(), root -> root.join(PurgeRequest_.document, JoinType.LEFT).get(Document_.id))
            );
        }
        return specification;
    }
}
