package cm.gimac.des.reporting.service;

import cm.gimac.des.reporting.domain.*; // for static metamodels
import cm.gimac.des.reporting.domain.DataExport;
import cm.gimac.des.reporting.repository.DataExportRepository;
import cm.gimac.des.reporting.service.criteria.DataExportCriteria;
import cm.gimac.des.reporting.service.dto.DataExportDTO;
import cm.gimac.des.reporting.service.mapper.DataExportMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Service for executing complex queries for {@link DataExport} entities in the database.
 * The main input is a {@link DataExportCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link DataExportDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class DataExportQueryService extends QueryService<DataExport> {

    private static final Logger LOG = LoggerFactory.getLogger(DataExportQueryService.class);

    private final DataExportRepository dataExportRepository;

    private final DataExportMapper dataExportMapper;

    public DataExportQueryService(DataExportRepository dataExportRepository, DataExportMapper dataExportMapper) {
        this.dataExportRepository = dataExportRepository;
        this.dataExportMapper = dataExportMapper;
    }

    /**
     * Return a {@link Page} of {@link DataExportDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<DataExportDTO> findByCriteria(DataExportCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<DataExport> specification = createSpecification(criteria);
        return dataExportRepository.findAll(specification, page).map(dataExportMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(DataExportCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<DataExport> specification = createSpecification(criteria);
        return dataExportRepository.count(specification);
    }

    /**
     * Function to convert {@link DataExportCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<DataExport> createSpecification(DataExportCriteria criteria) {
        Specification<DataExport> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), DataExport_.id),
                buildSpecification(criteria.getRequesterId(), DataExport_.requesterId),
                buildRangeSpecification(criteria.getRequestedAt(), DataExport_.requestedAt),
                buildStringSpecification(criteria.getIndicatorCodes(), DataExport_.indicatorCodes),
                buildStringSpecification(criteria.getPeriod(), DataExport_.period),
                buildRangeSpecification(criteria.getRowCount(), DataExport_.rowCount),
                buildStringSpecification(criteria.getWatermark(), DataExport_.watermark),
                buildStringSpecification(criteria.getContentDigest(), DataExport_.contentDigest),
                buildSpecification(criteria.getDocumentRef(), DataExport_.documentRef)
            );
        }
        return specification;
    }
}
