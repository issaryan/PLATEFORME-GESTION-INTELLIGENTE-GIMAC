package cm.gimac.des.certification.service;

import cm.gimac.des.certification.domain.*; // for static metamodels
import cm.gimac.des.certification.domain.ClosedOperationSnapshot;
import cm.gimac.des.certification.repository.ClosedOperationSnapshotRepository;
import cm.gimac.des.certification.service.criteria.ClosedOperationSnapshotCriteria;
import cm.gimac.des.certification.service.dto.ClosedOperationSnapshotDTO;
import cm.gimac.des.certification.service.mapper.ClosedOperationSnapshotMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Service for executing complex queries for {@link ClosedOperationSnapshot} entities in the database.
 * The main input is a {@link ClosedOperationSnapshotCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link ClosedOperationSnapshotDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class ClosedOperationSnapshotQueryService extends QueryService<ClosedOperationSnapshot> {

    private static final Logger LOG = LoggerFactory.getLogger(ClosedOperationSnapshotQueryService.class);

    private final ClosedOperationSnapshotRepository closedOperationSnapshotRepository;

    private final ClosedOperationSnapshotMapper closedOperationSnapshotMapper;

    public ClosedOperationSnapshotQueryService(
        ClosedOperationSnapshotRepository closedOperationSnapshotRepository,
        ClosedOperationSnapshotMapper closedOperationSnapshotMapper
    ) {
        this.closedOperationSnapshotRepository = closedOperationSnapshotRepository;
        this.closedOperationSnapshotMapper = closedOperationSnapshotMapper;
    }

    /**
     * Return a {@link Page} of {@link ClosedOperationSnapshotDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<ClosedOperationSnapshotDTO> findByCriteria(ClosedOperationSnapshotCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<ClosedOperationSnapshot> specification = createSpecification(criteria);
        return closedOperationSnapshotRepository.findAll(specification, page).map(closedOperationSnapshotMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(ClosedOperationSnapshotCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<ClosedOperationSnapshot> specification = createSpecification(criteria);
        return closedOperationSnapshotRepository.count(specification);
    }

    /**
     * Function to convert {@link ClosedOperationSnapshotCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<ClosedOperationSnapshot> createSpecification(ClosedOperationSnapshotCriteria criteria) {
        Specification<ClosedOperationSnapshot> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), ClosedOperationSnapshot_.id),
                buildSpecification(criteria.getLiaisonId(), ClosedOperationSnapshot_.liaisonId),
                buildSpecification(criteria.getOperationId(), ClosedOperationSnapshot_.operationId),
                buildStringSpecification(criteria.getOperationCode(), ClosedOperationSnapshot_.operationCode),
                buildStringSpecification(criteria.getReferenceCode(), ClosedOperationSnapshot_.referenceCode),
                buildStringSpecification(criteria.getServiceCode(), ClosedOperationSnapshot_.serviceCode),
                buildSpecification(criteria.getOwnerId(), ClosedOperationSnapshot_.ownerId),
                buildSpecification(criteria.getValidatorId(), ClosedOperationSnapshot_.validatorId),
                buildSpecification(criteria.getEvidenceDocumentId(), ClosedOperationSnapshot_.evidenceDocumentId),
                buildStringSpecification(criteria.getEvidenceDigest(), ClosedOperationSnapshot_.evidenceDigest),
                buildRangeSpecification(criteria.getClosedAt(), ClosedOperationSnapshot_.closedAt),
                buildSpecification(criteria.getSourceEventId(), ClosedOperationSnapshot_.sourceEventId)
            );
        }
        return specification;
    }
}
