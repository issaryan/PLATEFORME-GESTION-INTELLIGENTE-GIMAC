package cm.gimac.des.document.service;

import cm.gimac.des.document.domain.*; // for static metamodels
import cm.gimac.des.document.domain.DocumentTypePolicy;
import cm.gimac.des.document.repository.DocumentTypePolicyRepository;
import cm.gimac.des.document.service.criteria.DocumentTypePolicyCriteria;
import cm.gimac.des.document.service.dto.DocumentTypePolicyDTO;
import cm.gimac.des.document.service.mapper.DocumentTypePolicyMapper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Service for executing complex queries for {@link DocumentTypePolicy} entities in the database.
 * The main input is a {@link DocumentTypePolicyCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link List} of {@link DocumentTypePolicyDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class DocumentTypePolicyQueryService extends QueryService<DocumentTypePolicy> {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentTypePolicyQueryService.class);

    private final DocumentTypePolicyRepository documentTypePolicyRepository;

    private final DocumentTypePolicyMapper documentTypePolicyMapper;

    public DocumentTypePolicyQueryService(
        DocumentTypePolicyRepository documentTypePolicyRepository,
        DocumentTypePolicyMapper documentTypePolicyMapper
    ) {
        this.documentTypePolicyRepository = documentTypePolicyRepository;
        this.documentTypePolicyMapper = documentTypePolicyMapper;
    }

    /**
     * Return a {@link List} of {@link DocumentTypePolicyDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public List<DocumentTypePolicyDTO> findByCriteria(DocumentTypePolicyCriteria criteria) {
        LOG.debug("find by criteria : {}", criteria);
        final Specification<DocumentTypePolicy> specification = createSpecification(criteria);
        return documentTypePolicyMapper.toDto(documentTypePolicyRepository.findAll(specification));
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(DocumentTypePolicyCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<DocumentTypePolicy> specification = createSpecification(criteria);
        return documentTypePolicyRepository.count(specification);
    }

    /**
     * Function to convert {@link DocumentTypePolicyCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<DocumentTypePolicy> createSpecification(DocumentTypePolicyCriteria criteria) {
        Specification<DocumentTypePolicy> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), DocumentTypePolicy_.id),
                buildSpecification(criteria.getDocumentType(), DocumentTypePolicy_.documentType),
                buildRangeSpecification(criteria.getPolicyVersion(), DocumentTypePolicy_.policyVersion),
                buildStringSpecification(criteria.getRetentionPeriod(), DocumentTypePolicy_.retentionPeriod),
                buildSpecification(criteria.getRetentionMode(), DocumentTypePolicy_.retentionMode),
                buildSpecification(criteria.getMaskingRule(), DocumentTypePolicy_.maskingRule),
                buildSpecification(criteria.getRestrictedAccess(), DocumentTypePolicy_.restrictedAccess),
                buildSpecification(criteria.getPdfaRequired(), DocumentTypePolicy_.pdfaRequired),
                buildStringSpecification(criteria.getAllowedMediaTypes(), DocumentTypePolicy_.allowedMediaTypes),
                buildRangeSpecification(criteria.getMaxSizeBytes(), DocumentTypePolicy_.maxSizeBytes),
                buildStringSpecification(criteria.getAllowedCallers(), DocumentTypePolicy_.allowedCallers),
                buildRangeSpecification(criteria.getEffectiveFrom(), DocumentTypePolicy_.effectiveFrom),
                buildSpecification(criteria.getActive(), DocumentTypePolicy_.active)
            );
        }
        return specification;
    }
}
