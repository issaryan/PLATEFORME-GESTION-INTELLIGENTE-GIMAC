package cm.gimac.des.homologation.service;

import cm.gimac.des.homologation.domain.*; // for static metamodels
import cm.gimac.des.homologation.domain.ContractedScope;
import cm.gimac.des.homologation.repository.ContractedScopeRepository;
import cm.gimac.des.homologation.service.criteria.ContractedScopeCriteria;
import cm.gimac.des.homologation.service.dto.ContractedScopeDTO;
import cm.gimac.des.homologation.service.mapper.ContractedScopeMapper;
import jakarta.persistence.criteria.JoinType;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Service for executing complex queries for {@link ContractedScope} entities in the database.
 * The main input is a {@link ContractedScopeCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link List} of {@link ContractedScopeDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class ContractedScopeQueryService extends QueryService<ContractedScope> {

    private static final Logger LOG = LoggerFactory.getLogger(ContractedScopeQueryService.class);

    private final ContractedScopeRepository contractedScopeRepository;

    private final ContractedScopeMapper contractedScopeMapper;

    public ContractedScopeQueryService(ContractedScopeRepository contractedScopeRepository, ContractedScopeMapper contractedScopeMapper) {
        this.contractedScopeRepository = contractedScopeRepository;
        this.contractedScopeMapper = contractedScopeMapper;
    }

    /**
     * Return a {@link List} of {@link ContractedScopeDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public List<ContractedScopeDTO> findByCriteria(ContractedScopeCriteria criteria) {
        LOG.debug("find by criteria : {}", criteria);
        final Specification<ContractedScope> specification = createSpecification(criteria);
        return contractedScopeMapper.toDto(contractedScopeRepository.findAll(specification));
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(ContractedScopeCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<ContractedScope> specification = createSpecification(criteria);
        return contractedScopeRepository.count(specification);
    }

    /**
     * Function to convert {@link ContractedScopeCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<ContractedScope> createSpecification(ContractedScopeCriteria criteria) {
        Specification<ContractedScope> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), ContractedScope_.id),
                buildStringSpecification(criteria.getServiceCode(), ContractedScope_.serviceCode),
                buildSpecification(criteria.getIncluded(), ContractedScope_.included),
                buildSpecification(criteria.getProgress(), ContractedScope_.progress),
                buildRangeSpecification(criteria.getUpdatedAt(), ContractedScope_.updatedAt),
                buildSpecification(criteria.getLiaisonId(), root -> root.join(ContractedScope_.liaison, JoinType.LEFT).get(Liaison_.id))
            );
        }
        return specification;
    }
}
