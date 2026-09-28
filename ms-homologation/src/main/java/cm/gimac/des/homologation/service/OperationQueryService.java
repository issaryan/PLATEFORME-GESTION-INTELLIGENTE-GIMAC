package cm.gimac.des.homologation.service;

import cm.gimac.des.homologation.domain.*; // for static metamodels
import cm.gimac.des.homologation.domain.Operation;
import cm.gimac.des.homologation.repository.OperationRepository;
import cm.gimac.des.homologation.service.criteria.OperationCriteria;
import cm.gimac.des.homologation.service.dto.OperationDTO;
import cm.gimac.des.homologation.service.mapper.OperationMapper;
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
 * Service for executing complex queries for {@link Operation} entities in the database.
 * The main input is a {@link OperationCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link OperationDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class OperationQueryService extends QueryService<Operation> {

    private static final Logger LOG = LoggerFactory.getLogger(OperationQueryService.class);

    private final OperationRepository operationRepository;

    private final OperationMapper operationMapper;

    public OperationQueryService(OperationRepository operationRepository, OperationMapper operationMapper) {
        this.operationRepository = operationRepository;
        this.operationMapper = operationMapper;
    }

    /**
     * Return a {@link Page} of {@link OperationDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<OperationDTO> findByCriteria(OperationCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Operation> specification = createSpecification(criteria);
        return operationRepository.findAll(specification, page).map(operationMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(OperationCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Operation> specification = createSpecification(criteria);
        return operationRepository.count(specification);
    }

    /**
     * Function to convert {@link OperationCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Operation> createSpecification(OperationCriteria criteria) {
        Specification<Operation> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), Operation_.id),
                buildStringSpecification(criteria.getOperationCode(), Operation_.operationCode),
                buildSpecification(criteria.getState(), Operation_.state),
                buildSpecification(criteria.getOwnerId(), Operation_.ownerId),
                buildSpecification(criteria.getEvidenceDocumentId(), Operation_.evidenceDocumentId),
                buildStringSpecification(criteria.getEvidenceDigest(), Operation_.evidenceDigest),
                buildRangeSpecification(criteria.getEvidenceSealedAt(), Operation_.evidenceSealedAt),
                buildRangeSpecification(criteria.getEvidenceRetention(), Operation_.evidenceRetention),
                buildSpecification(criteria.getEvidenceSealConfirmed(), Operation_.evidenceSealConfirmed),
                buildStringSpecification(criteria.getEvidenceTraceRef(), Operation_.evidenceTraceRef),
                buildStringSpecification(criteria.getTimestampImprint(), Operation_.timestampImprint),
                buildRangeSpecification(criteria.getTimestampGenTime(), Operation_.timestampGenTime),
                buildSpecification(criteria.getValidatorId(), Operation_.validatorId),
                buildRangeSpecification(criteria.getValidatedAt(), Operation_.validatedAt),
                buildRangeSpecification(criteria.getClosedAt(), Operation_.closedAt),
                buildStringSpecification(criteria.getStateReason(), Operation_.stateReason),
                buildSpecification(criteria.getLiaisonId(), root -> root.join(Operation_.liaison, JoinType.LEFT).get(Liaison_.id)),
                buildSpecification(criteria.getReferenceId(), root ->
                    root.join(Operation_.reference, JoinType.LEFT).get(ReferenceOperation_.id)
                )
            );
        }
        return specification;
    }
}
