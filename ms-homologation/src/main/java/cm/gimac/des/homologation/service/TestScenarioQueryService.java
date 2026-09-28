package cm.gimac.des.homologation.service;

import cm.gimac.des.homologation.domain.*; // for static metamodels
import cm.gimac.des.homologation.domain.TestScenario;
import cm.gimac.des.homologation.repository.TestScenarioRepository;
import cm.gimac.des.homologation.service.criteria.TestScenarioCriteria;
import cm.gimac.des.homologation.service.dto.TestScenarioDTO;
import cm.gimac.des.homologation.service.mapper.TestScenarioMapper;
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
 * Service for executing complex queries for {@link TestScenario} entities in the database.
 * The main input is a {@link TestScenarioCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link TestScenarioDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class TestScenarioQueryService extends QueryService<TestScenario> {

    private static final Logger LOG = LoggerFactory.getLogger(TestScenarioQueryService.class);

    private final TestScenarioRepository testScenarioRepository;

    private final TestScenarioMapper testScenarioMapper;

    public TestScenarioQueryService(TestScenarioRepository testScenarioRepository, TestScenarioMapper testScenarioMapper) {
        this.testScenarioRepository = testScenarioRepository;
        this.testScenarioMapper = testScenarioMapper;
    }

    /**
     * Return a {@link Page} of {@link TestScenarioDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<TestScenarioDTO> findByCriteria(TestScenarioCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<TestScenario> specification = createSpecification(criteria);
        return testScenarioRepository.findAll(specification, page).map(testScenarioMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(TestScenarioCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<TestScenario> specification = createSpecification(criteria);
        return testScenarioRepository.count(specification);
    }

    /**
     * Function to convert {@link TestScenarioCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<TestScenario> createSpecification(TestScenarioCriteria criteria) {
        Specification<TestScenario> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), TestScenario_.id),
                buildStringSpecification(criteria.getSimulatorScenarioId(), TestScenario_.simulatorScenarioId),
                buildSpecification(criteria.getOutcome(), TestScenario_.outcome),
                buildStringSpecification(criteria.getReason(), TestScenario_.reason),
                buildRangeSpecification(criteria.getExecutedAt(), TestScenario_.executedAt),
                buildStringSpecification(criteria.getTraceRef(), TestScenario_.traceRef),
                buildRangeSpecification(criteria.getReplayedAt(), TestScenario_.replayedAt),
                buildSpecification(criteria.getMatch(), TestScenario_.match),
                buildSpecification(criteria.getLiaisonId(), root -> root.join(TestScenario_.liaison, JoinType.LEFT).get(Liaison_.id)),
                buildSpecification(criteria.getOperationId(), root -> root.join(TestScenario_.operation, JoinType.LEFT).get(Operation_.id))
            );
        }
        return specification;
    }
}
