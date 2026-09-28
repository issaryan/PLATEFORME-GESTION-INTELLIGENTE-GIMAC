package cm.gimac.des.homologation.service;

import cm.gimac.des.homologation.domain.*; // for static metamodels
import cm.gimac.des.homologation.domain.ReferenceOperation;
import cm.gimac.des.homologation.repository.ReferenceOperationRepository;
import cm.gimac.des.homologation.service.criteria.ReferenceOperationCriteria;
import cm.gimac.des.homologation.service.dto.ReferenceOperationDTO;
import cm.gimac.des.homologation.service.mapper.ReferenceOperationMapper;
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
 * Service for executing complex queries for {@link ReferenceOperation} entities in the database.
 * The main input is a {@link ReferenceOperationCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link ReferenceOperationDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class ReferenceOperationQueryService extends QueryService<ReferenceOperation> {

    private static final Logger LOG = LoggerFactory.getLogger(ReferenceOperationQueryService.class);

    private final ReferenceOperationRepository referenceOperationRepository;

    private final ReferenceOperationMapper referenceOperationMapper;

    public ReferenceOperationQueryService(
        ReferenceOperationRepository referenceOperationRepository,
        ReferenceOperationMapper referenceOperationMapper
    ) {
        this.referenceOperationRepository = referenceOperationRepository;
        this.referenceOperationMapper = referenceOperationMapper;
    }

    /**
     * Return a {@link Page} of {@link ReferenceOperationDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<ReferenceOperationDTO> findByCriteria(ReferenceOperationCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<ReferenceOperation> specification = createSpecification(criteria);
        return referenceOperationRepository
            .fetchBagRelationships(referenceOperationRepository.findAll(specification, page))
            .map(referenceOperationMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(ReferenceOperationCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<ReferenceOperation> specification = createSpecification(criteria);
        return referenceOperationRepository.count(specification);
    }

    /**
     * Function to convert {@link ReferenceOperationCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<ReferenceOperation> createSpecification(ReferenceOperationCriteria criteria) {
        Specification<ReferenceOperation> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), ReferenceOperation_.id),
                buildStringSpecification(criteria.getCode(), ReferenceOperation_.code),
                buildStringSpecification(criteria.getLabel(), ReferenceOperation_.label),
                buildStringSpecification(criteria.getBranch(), ReferenceOperation_.branch),
                buildSpecification(criteria.getPhase(), ReferenceOperation_.phase),
                buildSpecification(criteria.getOwner(), ReferenceOperation_.owner),
                buildStringSpecification(criteria.getExpectedEvidence(), ReferenceOperation_.expectedEvidence),
                buildStringSpecification(criteria.getServiceCode(), ReferenceOperation_.serviceCode),
                buildStringSpecification(criteria.getScenarioRef(), ReferenceOperation_.scenarioRef),
                buildSpecification(criteria.getCriticalPath(), ReferenceOperation_.criticalPath),
                buildSpecification(criteria.getCatalogId(), root ->
                    root.join(ReferenceOperation_.catalog, JoinType.LEFT).get(ReferenceCatalog_.id)
                ),
                buildSpecification(criteria.getPredecessorsId(), root ->
                    root.join(ReferenceOperation_.predecessors, JoinType.LEFT).get(ReferenceOperation_.id)
                ),
                buildSpecification(criteria.getSuccessorsId(), root ->
                    root.join(ReferenceOperation_.successors, JoinType.LEFT).get(ReferenceOperation_.id)
                )
            );
        }
        return specification;
    }
}
