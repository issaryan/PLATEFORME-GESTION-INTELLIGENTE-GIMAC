package cm.gimac.des.rapprochement.service;

import cm.gimac.des.rapprochement.domain.*; // for static metamodels
import cm.gimac.des.rapprochement.domain.ImportedStatement;
import cm.gimac.des.rapprochement.repository.ImportedStatementRepository;
import cm.gimac.des.rapprochement.service.criteria.ImportedStatementCriteria;
import cm.gimac.des.rapprochement.service.dto.ImportedStatementDTO;
import cm.gimac.des.rapprochement.service.mapper.ImportedStatementMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Service for executing complex queries for {@link ImportedStatement} entities in the database.
 * The main input is a {@link ImportedStatementCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link ImportedStatementDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class ImportedStatementQueryService extends QueryService<ImportedStatement> {

    private static final Logger LOG = LoggerFactory.getLogger(ImportedStatementQueryService.class);

    private final ImportedStatementRepository importedStatementRepository;

    private final ImportedStatementMapper importedStatementMapper;

    public ImportedStatementQueryService(
        ImportedStatementRepository importedStatementRepository,
        ImportedStatementMapper importedStatementMapper
    ) {
        this.importedStatementRepository = importedStatementRepository;
        this.importedStatementMapper = importedStatementMapper;
    }

    /**
     * Return a {@link Page} of {@link ImportedStatementDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<ImportedStatementDTO> findByCriteria(ImportedStatementCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<ImportedStatement> specification = createSpecification(criteria);
        return importedStatementRepository.findAll(specification, page).map(importedStatementMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(ImportedStatementCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<ImportedStatement> specification = createSpecification(criteria);
        return importedStatementRepository.count(specification);
    }

    /**
     * Function to convert {@link ImportedStatementCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<ImportedStatement> createSpecification(ImportedStatementCriteria criteria) {
        Specification<ImportedStatement> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), ImportedStatement_.id),
                buildStringSpecification(criteria.getReference(), ImportedStatement_.reference),
                buildSpecification(criteria.getSource(), ImportedStatement_.source),
                buildSpecification(criteria.getInstitutionId(), ImportedStatement_.institutionId),
                buildRangeSpecification(criteria.getBusinessDate(), ImportedStatement_.businessDate),
                buildRangeSpecification(criteria.getImportedAt(), ImportedStatement_.importedAt),
                buildSpecification(criteria.getImporterId(), ImportedStatement_.importerId),
                buildSpecification(criteria.getFormat(), ImportedStatement_.format),
                buildStringSpecification(criteria.getDigest(), ImportedStatement_.digest),
                buildSpecification(criteria.getMaskingPassed(), ImportedStatement_.maskingPassed),
                buildRangeSpecification(criteria.getMaskingRejectedLines(), ImportedStatement_.maskingRejectedLines),
                buildRangeSpecification(criteria.getMaskingCheckedAt(), ImportedStatement_.maskingCheckedAt),
                buildRangeSpecification(criteria.getLineCount(), ImportedStatement_.lineCount),
                buildSpecification(criteria.getDocumentRef(), ImportedStatement_.documentRef),
                buildRangeSpecification(criteria.getLinesRetainedUntil(), ImportedStatement_.linesRetainedUntil)
            );
        }
        return specification;
    }
}
