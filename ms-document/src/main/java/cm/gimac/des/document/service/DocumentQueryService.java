package cm.gimac.des.document.service;

import cm.gimac.des.document.domain.*; // for static metamodels
import cm.gimac.des.document.domain.Document;
import cm.gimac.des.document.repository.DocumentRepository;
import cm.gimac.des.document.service.criteria.DocumentCriteria;
import cm.gimac.des.document.service.dto.DocumentDTO;
import cm.gimac.des.document.service.mapper.DocumentMapper;
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
 * Service for executing complex queries for {@link Document} entities in the database.
 * The main input is a {@link DocumentCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link DocumentDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class DocumentQueryService extends QueryService<Document> {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentQueryService.class);

    private final DocumentRepository documentRepository;

    private final DocumentMapper documentMapper;

    public DocumentQueryService(DocumentRepository documentRepository, DocumentMapper documentMapper) {
        this.documentRepository = documentRepository;
        this.documentMapper = documentMapper;
    }

    /**
     * Return a {@link Page} of {@link DocumentDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<DocumentDTO> findByCriteria(DocumentCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Document> specification = createSpecification(criteria);
        return documentRepository.findAll(specification, page).map(documentMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(DocumentCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Document> specification = createSpecification(criteria);
        return documentRepository.count(specification);
    }

    /**
     * Function to convert {@link DocumentCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Document> createSpecification(DocumentCriteria criteria) {
        Specification<Document> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), Document_.id),
                buildSpecification(criteria.getType(), Document_.type),
                buildStringSpecification(criteria.getTitle(), Document_.title),
                buildRangeSpecification(criteria.getVersion(), Document_.version),
                buildSpecification(criteria.getAuthorId(), Document_.authorId),
                buildStringSpecification(criteria.getCallerService(), Document_.callerService),
                buildSpecification(criteria.getInstitutionId(), Document_.institutionId),
                buildSpecification(criteria.getVisibility(), Document_.visibility),
                buildStringSpecification(criteria.getOwnerUnit(), Document_.ownerUnit),
                buildStringSpecification(criteria.getRestrictedReaderIds(), Document_.restrictedReaderIds),
                buildRangeSpecification(criteria.getDepositedAt(), Document_.depositedAt),
                buildStringSpecification(criteria.getSha256(), Document_.sha256),
                buildRangeSpecification(criteria.getSizeBytes(), Document_.sizeBytes),
                buildStringSpecification(criteria.getMediaType(), Document_.mediaType),
                buildStringSpecification(criteria.getObjectKey(), Document_.objectKey),
                buildSpecification(criteria.getState(), Document_.state),
                buildStringSpecification(criteria.getRetentionPeriod(), Document_.retentionPeriod),
                buildSpecification(criteria.getRetentionMode(), Document_.retentionMode),
                buildSpecification(criteria.getLegalHold(), Document_.legalHold),
                buildRangeSpecification(criteria.getRetainUntil(), Document_.retainUntil),
                buildRangeSpecification(criteria.getLockedAt(), Document_.lockedAt),
                buildSpecification(criteria.getScanClean(), Document_.scanClean),
                buildStringSpecification(criteria.getScanSignatureName(), Document_.scanSignatureName),
                buildStringSpecification(criteria.getScanEngineVersion(), Document_.scanEngineVersion),
                buildRangeSpecification(criteria.getScannedAt(), Document_.scannedAt),
                buildRangeSpecification(criteria.getMaskingCandidates(), Document_.maskingCandidates),
                buildRangeSpecification(criteria.getMaskingLuhnValid(), Document_.maskingLuhnValid),
                buildRangeSpecification(criteria.getMaskingMasked(), Document_.maskingMasked),
                buildSpecification(criteria.getMaskingRejected(), Document_.maskingRejected),
                buildSpecification(criteria.getPdfaCompliant(), Document_.pdfaCompliant),
                buildStringSpecification(criteria.getRejectionReason(), Document_.rejectionReason),
                buildRangeSpecification(criteria.getSealedAt(), Document_.sealedAt),
                buildRangeSpecification(criteria.getPurgedAt(), Document_.purgedAt),
                buildSpecification(criteria.getLineageId(), root -> root.join(Document_.lineage, JoinType.LEFT).get(DocumentLineage_.id)),
                buildSpecification(criteria.getPolicyId(), root -> root.join(Document_.policy, JoinType.LEFT).get(DocumentTypePolicy_.id)),
                buildSpecification(criteria.getDocumentLineageId(), root ->
                    root.join(Document_.documentLineage, JoinType.LEFT).get(DocumentLineage_.id)
                )
            );
        }
        return specification;
    }
}
