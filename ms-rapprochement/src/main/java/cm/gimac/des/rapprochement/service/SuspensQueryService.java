package cm.gimac.des.rapprochement.service;

import cm.gimac.des.rapprochement.domain.*; // for static metamodels
import cm.gimac.des.rapprochement.domain.Suspens;
import cm.gimac.des.rapprochement.repository.SuspensRepository;
import cm.gimac.des.rapprochement.service.criteria.SuspensCriteria;
import cm.gimac.des.rapprochement.service.dto.SuspensDTO;
import cm.gimac.des.rapprochement.service.mapper.SuspensMapper;
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
 * Service for executing complex queries for {@link Suspens} entities in the database.
 * The main input is a {@link SuspensCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link SuspensDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class SuspensQueryService extends QueryService<Suspens> {

    private static final Logger LOG = LoggerFactory.getLogger(SuspensQueryService.class);

    private final SuspensRepository suspensRepository;

    private final SuspensMapper suspensMapper;

    public SuspensQueryService(SuspensRepository suspensRepository, SuspensMapper suspensMapper) {
        this.suspensRepository = suspensRepository;
        this.suspensMapper = suspensMapper;
    }

    /**
     * Return a {@link Page} of {@link SuspensDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<SuspensDTO> findByCriteria(SuspensCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Suspens> specification = createSpecification(criteria);
        return suspensRepository.findAll(specification, page).map(suspensMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(SuspensCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Suspens> specification = createSpecification(criteria);
        return suspensRepository.count(specification);
    }

    /**
     * Function to convert {@link SuspensCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Suspens> createSpecification(SuspensCriteria criteria) {
        Specification<Suspens> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), Suspens_.id),
                buildStringSpecification(criteria.getReference(), Suspens_.reference),
                buildSpecification(criteria.getInstitutionId(), Suspens_.institutionId),
                buildSpecification(criteria.getType(), Suspens_.type),
                buildStringSpecification(criteria.getPivotTransactionRef(), Suspens_.pivotTransactionRef),
                buildStringSpecification(criteria.getPivotAuthorizationRef(), Suspens_.pivotAuthorizationRef),
                buildRangeSpecification(criteria.getPivotOperationAt(), Suspens_.pivotOperationAt),
                buildRangeSpecification(criteria.getPivotAmount(), Suspens_.pivotAmount),
                buildStringSpecification(criteria.getPivotParticipantCode(), Suspens_.pivotParticipantCode),
                buildRangeSpecification(criteria.getAmountSwitch(), Suspens_.amountSwitch),
                buildRangeSpecification(criteria.getAmountParticipant(), Suspens_.amountParticipant),
                buildSpecification(criteria.getState(), Suspens_.state),
                buildSpecification(criteria.getAssigneeId(), Suspens_.assigneeId),
                buildRangeSpecification(criteria.getDueDate(), Suspens_.dueDate),
                buildRangeSpecification(criteria.getOpenedAt(), Suspens_.openedAt),
                buildSpecification(criteria.getDisputeTicketId(), Suspens_.disputeTicketId),
                buildRangeSpecification(criteria.getClosedAt(), Suspens_.closedAt),
                buildSpecification(criteria.getReconciliationId(), root ->
                    root.join(Suspens_.reconciliation, JoinType.LEFT).get(Reconciliation_.id)
                ),
                buildSpecification(criteria.getRegularizationAttestationId(), root ->
                    root.join(Suspens_.regularizationAttestation, JoinType.LEFT).get(RegularizationAttestation_.id)
                )
            );
        }
        return specification;
    }
}
