package cm.gimac.des.reporting.service;

import cm.gimac.des.reporting.domain.*; // for static metamodels
import cm.gimac.des.reporting.domain.Fact;
import cm.gimac.des.reporting.repository.FactRepository;
import cm.gimac.des.reporting.service.criteria.FactCriteria;
import cm.gimac.des.reporting.service.dto.FactDTO;
import cm.gimac.des.reporting.service.mapper.FactMapper;
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
 * Service for executing complex queries for {@link Fact} entities in the database.
 * The main input is a {@link FactCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link FactDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class FactQueryService extends QueryService<Fact> {

    private static final Logger LOG = LoggerFactory.getLogger(FactQueryService.class);

    private final FactRepository factRepository;

    private final FactMapper factMapper;

    public FactQueryService(FactRepository factRepository, FactMapper factMapper) {
        this.factRepository = factRepository;
        this.factMapper = factMapper;
    }

    /**
     * Return a {@link Page} of {@link FactDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<FactDTO> findByCriteria(FactCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Fact> specification = createSpecification(criteria);
        return factRepository.findAll(specification, page).map(factMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(FactCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Fact> specification = createSpecification(criteria);
        return factRepository.count(specification);
    }

    /**
     * Function to convert {@link FactCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Fact> createSpecification(FactCriteria criteria) {
        Specification<Fact> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), Fact_.id),
                buildSpecification(criteria.getDomain(), Fact_.domain),
                buildRangeSpecification(criteria.getTimeKey(), Fact_.timeKey),
                buildSpecification(criteria.getInstitutionId(), Fact_.institutionId),
                buildStringSpecification(criteria.getServiceCode(), Fact_.serviceCode),
                buildStringSpecification(criteria.getSeverity(), Fact_.severity),
                buildSpecification(criteria.getSourceEventId(), Fact_.sourceEventId),
                buildRangeSpecification(criteria.getRecordedAt(), Fact_.recordedAt),
                buildSpecification(criteria.getSuperseded(), Fact_.superseded),
                buildSpecification(criteria.getCompacted(), Fact_.compacted),
                buildSpecification(criteria.getStatementId(), root -> root.join(Fact_.statement, JoinType.LEFT).get(ImportedStatement_.id))
            );
        }
        return specification;
    }
}
