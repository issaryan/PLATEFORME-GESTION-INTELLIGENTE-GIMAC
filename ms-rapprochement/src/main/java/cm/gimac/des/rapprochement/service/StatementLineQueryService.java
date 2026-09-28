package cm.gimac.des.rapprochement.service;

import cm.gimac.des.rapprochement.domain.*; // for static metamodels
import cm.gimac.des.rapprochement.domain.StatementLine;
import cm.gimac.des.rapprochement.repository.StatementLineRepository;
import cm.gimac.des.rapprochement.service.criteria.StatementLineCriteria;
import cm.gimac.des.rapprochement.service.dto.StatementLineDTO;
import cm.gimac.des.rapprochement.service.mapper.StatementLineMapper;
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
 * Service for executing complex queries for {@link StatementLine} entities in the database.
 * The main input is a {@link StatementLineCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link StatementLineDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class StatementLineQueryService extends QueryService<StatementLine> {

    private static final Logger LOG = LoggerFactory.getLogger(StatementLineQueryService.class);

    private final StatementLineRepository statementLineRepository;

    private final StatementLineMapper statementLineMapper;

    public StatementLineQueryService(StatementLineRepository statementLineRepository, StatementLineMapper statementLineMapper) {
        this.statementLineRepository = statementLineRepository;
        this.statementLineMapper = statementLineMapper;
    }

    /**
     * Return a {@link Page} of {@link StatementLineDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<StatementLineDTO> findByCriteria(StatementLineCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<StatementLine> specification = createSpecification(criteria);
        return statementLineRepository.findAll(specification, page).map(statementLineMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(StatementLineCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<StatementLine> specification = createSpecification(criteria);
        return statementLineRepository.count(specification);
    }

    /**
     * Function to convert {@link StatementLineCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<StatementLine> createSpecification(StatementLineCriteria criteria) {
        Specification<StatementLine> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), StatementLine_.id),
                buildRangeSpecification(criteria.getLineNumber(), StatementLine_.lineNumber),
                buildStringSpecification(criteria.getTransactionRef(), StatementLine_.transactionRef),
                buildStringSpecification(criteria.getAuthorizationRef(), StatementLine_.authorizationRef),
                buildRangeSpecification(criteria.getOperationAt(), StatementLine_.operationAt),
                buildRangeSpecification(criteria.getAmount(), StatementLine_.amount),
                buildStringSpecification(criteria.getParticipantCode(), StatementLine_.participantCode),
                buildStringSpecification(criteria.getLineStatus(), StatementLine_.lineStatus),
                buildStringSpecification(criteria.getCurrency(), StatementLine_.currency),
                buildSpecification(criteria.getStatementId(), root ->
                    root.join(StatementLine_.statement, JoinType.LEFT).get(ImportedStatement_.id)
                )
            );
        }
        return specification;
    }
}
