package cm.gimac.des.audit.service;

import cm.gimac.des.audit.domain.*; // for static metamodels
import cm.gimac.des.audit.domain.PeriodRoot;
import cm.gimac.des.audit.repository.PeriodRootRepository;
import cm.gimac.des.audit.service.criteria.PeriodRootCriteria;
import cm.gimac.des.audit.service.dto.PeriodRootDTO;
import cm.gimac.des.audit.service.mapper.PeriodRootMapper;
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
 * Service for executing complex queries for {@link PeriodRoot} entities in the database.
 * The main input is a {@link PeriodRootCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link PeriodRootDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class PeriodRootQueryService extends QueryService<PeriodRoot> {

    private static final Logger LOG = LoggerFactory.getLogger(PeriodRootQueryService.class);

    private final PeriodRootRepository periodRootRepository;

    private final PeriodRootMapper periodRootMapper;

    public PeriodRootQueryService(PeriodRootRepository periodRootRepository, PeriodRootMapper periodRootMapper) {
        this.periodRootRepository = periodRootRepository;
        this.periodRootMapper = periodRootMapper;
    }

    /**
     * Return a {@link Page} of {@link PeriodRootDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<PeriodRootDTO> findByCriteria(PeriodRootCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<PeriodRoot> specification = createSpecification(criteria);
        return periodRootRepository.findAll(specification, page).map(periodRootMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(PeriodRootCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<PeriodRoot> specification = createSpecification(criteria);
        return periodRootRepository.count(specification);
    }

    /**
     * Function to convert {@link PeriodRootCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<PeriodRoot> createSpecification(PeriodRootCriteria criteria) {
        Specification<PeriodRoot> specification = Specification.unrestricted();
        specification = specification.and((root, query, builder) -> {
            if (Long.class != query.getResultType()) {
                root.fetch(PeriodRoot_.previousRoot, JoinType.LEFT);
                root.fetch(PeriodRoot_.consistencyWithPrevious, JoinType.LEFT);
            }
            return null;
        });
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = specification.and(
                Specification.allOf(
                    Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : Specification.unrestricted(),
                    buildSpecification(criteria.getId(), PeriodRoot_.id),
                    buildStringSpecification(criteria.getPeriod(), PeriodRoot_.period),
                    buildRangeSpecification(criteria.getTreeSize(), PeriodRoot_.treeSize),
                    buildStringSpecification(criteria.getRootHash(), PeriodRoot_.rootHash),
                    buildRangeSpecification(criteria.getSealedAt(), PeriodRoot_.sealedAt),
                    buildSpecification(criteria.getState(), PeriodRoot_.state),
                    buildStringSpecification(criteria.getTimestampSerialNumber(), PeriodRoot_.timestampSerialNumber),
                    buildRangeSpecification(criteria.getTimestampGenTime(), PeriodRoot_.timestampGenTime),
                    buildStringSpecification(criteria.getTimestampImprint(), PeriodRoot_.timestampImprint),
                    buildStringSpecification(criteria.getPublicationReference(), PeriodRoot_.publicationReference),
                    buildRangeSpecification(criteria.getPublishedAt(), PeriodRoot_.publishedAt),
                    buildSpecification(criteria.getPreviousRootId(), root ->
                        root.join(PeriodRoot_.previousRoot, JoinType.LEFT).get(PeriodRoot_.id)
                    ),
                    buildSpecification(criteria.getConsistencyWithPreviousId(), root ->
                        root.join(PeriodRoot_.consistencyWithPrevious, JoinType.LEFT).get(ConsistencyProof_.id)
                    ),
                    buildSpecification(criteria.getPeriodRootId(), root ->
                        root.join(PeriodRoot_.periodRoot, JoinType.LEFT).get(PeriodRoot_.id)
                    )
                )
            );
        }
        return specification;
    }
}
