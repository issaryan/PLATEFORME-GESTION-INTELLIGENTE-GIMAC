package cm.gimac.des.rapprochement.service;

import cm.gimac.des.rapprochement.domain.*; // for static metamodels
import cm.gimac.des.rapprochement.domain.Reconciliation;
import cm.gimac.des.rapprochement.repository.ReconciliationRepository;
import cm.gimac.des.rapprochement.service.criteria.ReconciliationCriteria;
import cm.gimac.des.rapprochement.service.dto.ReconciliationDTO;
import cm.gimac.des.rapprochement.service.mapper.ReconciliationMapper;
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
 * Service for executing complex queries for {@link Reconciliation} entities in the database.
 * The main input is a {@link ReconciliationCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link ReconciliationDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class ReconciliationQueryService extends QueryService<Reconciliation> {

    private static final Logger LOG = LoggerFactory.getLogger(ReconciliationQueryService.class);

    private final ReconciliationRepository reconciliationRepository;

    private final ReconciliationMapper reconciliationMapper;

    public ReconciliationQueryService(ReconciliationRepository reconciliationRepository, ReconciliationMapper reconciliationMapper) {
        this.reconciliationRepository = reconciliationRepository;
        this.reconciliationMapper = reconciliationMapper;
    }

    /**
     * Return a {@link Page} of {@link ReconciliationDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<ReconciliationDTO> findByCriteria(ReconciliationCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Reconciliation> specification = createSpecification(criteria);
        return reconciliationRepository.findAll(specification, page).map(reconciliationMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(ReconciliationCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Reconciliation> specification = createSpecification(criteria);
        return reconciliationRepository.count(specification);
    }

    /**
     * Function to convert {@link ReconciliationCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Reconciliation> createSpecification(ReconciliationCriteria criteria) {
        Specification<Reconciliation> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), Reconciliation_.id),
                buildStringSpecification(criteria.getReference(), Reconciliation_.reference),
                buildRangeSpecification(criteria.getBusinessDate(), Reconciliation_.businessDate),
                buildSpecification(criteria.getInstitutionId(), Reconciliation_.institutionId),
                buildSpecification(criteria.getState(), Reconciliation_.state),
                buildRangeSpecification(criteria.getTimestampTolerance(), Reconciliation_.timestampTolerance),
                buildRangeSpecification(criteria.getMatchedCount(), Reconciliation_.matchedCount),
                buildRangeSpecification(criteria.getDiscrepancyCount(), Reconciliation_.discrepancyCount),
                buildRangeSpecification(criteria.getStartedAt(), Reconciliation_.startedAt),
                buildRangeSpecification(criteria.getCompletedAt(), Reconciliation_.completedAt),
                buildSpecification(criteria.getClosedBy(), Reconciliation_.closedBy),
                buildRangeSpecification(criteria.getClosedAt(), Reconciliation_.closedAt),
                buildSpecification(criteria.getSwitchStatementId(), root ->
                    root.join(Reconciliation_.switchStatement, JoinType.LEFT).get(ImportedStatement_.id)
                ),
                buildSpecification(criteria.getParticipantStatementId(), root ->
                    root.join(Reconciliation_.participantStatement, JoinType.LEFT).get(ImportedStatement_.id)
                ),
                buildSpecification(criteria.getSettlementStatementId(), root ->
                    root.join(Reconciliation_.settlementStatement, JoinType.LEFT).get(ImportedStatement_.id)
                )
            );
        }
        return specification;
    }
}
