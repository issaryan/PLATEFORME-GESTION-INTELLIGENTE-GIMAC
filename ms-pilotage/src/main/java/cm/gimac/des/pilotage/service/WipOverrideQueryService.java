package cm.gimac.des.pilotage.service;

import cm.gimac.des.pilotage.domain.*; // for static metamodels
import cm.gimac.des.pilotage.domain.WipOverride;
import cm.gimac.des.pilotage.repository.WipOverrideRepository;
import cm.gimac.des.pilotage.service.criteria.WipOverrideCriteria;
import cm.gimac.des.pilotage.service.dto.WipOverrideDTO;
import cm.gimac.des.pilotage.service.mapper.WipOverrideMapper;
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
 * Service for executing complex queries for {@link WipOverride} entities in the database.
 * The main input is a {@link WipOverrideCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link WipOverrideDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class WipOverrideQueryService extends QueryService<WipOverride> {

    private static final Logger LOG = LoggerFactory.getLogger(WipOverrideQueryService.class);

    private final WipOverrideRepository wipOverrideRepository;

    private final WipOverrideMapper wipOverrideMapper;

    public WipOverrideQueryService(WipOverrideRepository wipOverrideRepository, WipOverrideMapper wipOverrideMapper) {
        this.wipOverrideRepository = wipOverrideRepository;
        this.wipOverrideMapper = wipOverrideMapper;
    }

    /**
     * Return a {@link Page} of {@link WipOverrideDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<WipOverrideDTO> findByCriteria(WipOverrideCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<WipOverride> specification = createSpecification(criteria);
        return wipOverrideRepository.findAll(specification, page).map(wipOverrideMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(WipOverrideCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<WipOverride> specification = createSpecification(criteria);
        return wipOverrideRepository.count(specification);
    }

    /**
     * Function to convert {@link WipOverrideCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<WipOverride> createSpecification(WipOverrideCriteria criteria) {
        Specification<WipOverride> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), WipOverride_.id),
                buildSpecification(criteria.getRequesterId(), WipOverride_.requesterId),
                buildSpecification(criteria.getApproverId(), WipOverride_.approverId),
                buildStringSpecification(criteria.getReason(), WipOverride_.reason),
                buildRangeSpecification(criteria.getGrantedAt(), WipOverride_.grantedAt),
                buildRangeSpecification(criteria.getExpiresAt(), WipOverride_.expiresAt),
                buildRangeSpecification(criteria.getExpiryNotifiedAt(), WipOverride_.expiryNotifiedAt),
                buildSpecification(criteria.getBoardColumnId(), root ->
                    root.join(WipOverride_.boardColumn, JoinType.LEFT).get(BoardColumn_.id)
                ),
                buildSpecification(criteria.getTaskId(), root -> root.join(WipOverride_.task, JoinType.LEFT).get(Task_.id))
            );
        }
        return specification;
    }
}
