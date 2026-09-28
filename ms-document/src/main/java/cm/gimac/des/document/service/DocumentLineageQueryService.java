package cm.gimac.des.document.service;

import cm.gimac.des.document.domain.*; // for static metamodels
import cm.gimac.des.document.domain.DocumentLineage;
import cm.gimac.des.document.repository.DocumentLineageRepository;
import cm.gimac.des.document.service.criteria.DocumentLineageCriteria;
import cm.gimac.des.document.service.dto.DocumentLineageDTO;
import cm.gimac.des.document.service.mapper.DocumentLineageMapper;
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
 * Service for executing complex queries for {@link DocumentLineage} entities in the database.
 * The main input is a {@link DocumentLineageCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link DocumentLineageDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class DocumentLineageQueryService extends QueryService<DocumentLineage> {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentLineageQueryService.class);

    private final DocumentLineageRepository documentLineageRepository;

    private final DocumentLineageMapper documentLineageMapper;

    public DocumentLineageQueryService(DocumentLineageRepository documentLineageRepository, DocumentLineageMapper documentLineageMapper) {
        this.documentLineageRepository = documentLineageRepository;
        this.documentLineageMapper = documentLineageMapper;
    }

    /**
     * Return a {@link Page} of {@link DocumentLineageDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<DocumentLineageDTO> findByCriteria(DocumentLineageCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<DocumentLineage> specification = createSpecification(criteria);
        return documentLineageRepository.findAll(specification, page).map(documentLineageMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(DocumentLineageCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<DocumentLineage> specification = createSpecification(criteria);
        return documentLineageRepository.count(specification);
    }

    /**
     * Function to convert {@link DocumentLineageCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<DocumentLineage> createSpecification(DocumentLineageCriteria criteria) {
        Specification<DocumentLineage> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), DocumentLineage_.id),
                buildSpecification(criteria.getType(), DocumentLineage_.type),
                buildStringSpecification(criteria.getOwnerService(), DocumentLineage_.ownerService),
                buildSpecification(criteria.getOwnerId(), DocumentLineage_.ownerId),
                buildSpecification(criteria.getInstitutionId(), DocumentLineage_.institutionId),
                buildRangeSpecification(criteria.getCurrentVersion(), DocumentLineage_.currentVersion),
                buildRangeSpecification(criteria.getCreatedAt(), DocumentLineage_.createdAt),
                buildSpecification(criteria.getCurrentDocumentId(), root ->
                    root.join(DocumentLineage_.currentDocument, JoinType.LEFT).get(Document_.id)
                )
            );
        }
        return specification;
    }
}
