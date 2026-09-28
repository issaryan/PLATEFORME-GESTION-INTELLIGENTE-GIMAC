package cm.gimac.des.reporting.service;

import cm.gimac.des.reporting.domain.*; // for static metamodels
import cm.gimac.des.reporting.domain.Dimension;
import cm.gimac.des.reporting.repository.DimensionRepository;
import cm.gimac.des.reporting.service.criteria.DimensionCriteria;
import cm.gimac.des.reporting.service.dto.DimensionDTO;
import cm.gimac.des.reporting.service.mapper.DimensionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Service for executing complex queries for {@link Dimension} entities in the database.
 * The main input is a {@link DimensionCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link DimensionDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class DimensionQueryService extends QueryService<Dimension> {

    private static final Logger LOG = LoggerFactory.getLogger(DimensionQueryService.class);

    private final DimensionRepository dimensionRepository;

    private final DimensionMapper dimensionMapper;

    public DimensionQueryService(DimensionRepository dimensionRepository, DimensionMapper dimensionMapper) {
        this.dimensionRepository = dimensionRepository;
        this.dimensionMapper = dimensionMapper;
    }

    /**
     * Return a {@link Page} of {@link DimensionDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<DimensionDTO> findByCriteria(DimensionCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Dimension> specification = createSpecification(criteria);
        return dimensionRepository.findAll(specification, page).map(dimensionMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(DimensionCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Dimension> specification = createSpecification(criteria);
        return dimensionRepository.count(specification);
    }

    /**
     * Function to convert {@link DimensionCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Dimension> createSpecification(DimensionCriteria criteria) {
        Specification<Dimension> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), Dimension_.id),
                buildSpecification(criteria.getKind(), Dimension_.kind),
                buildStringSpecification(criteria.getKey(), Dimension_.key),
                buildStringSpecification(criteria.getLabel(), Dimension_.label),
                buildStringSpecification(criteria.getParentKey(), Dimension_.parentKey),
                buildRangeSpecification(criteria.getUpdatedAt(), Dimension_.updatedAt)
            );
        }
        return specification;
    }
}
