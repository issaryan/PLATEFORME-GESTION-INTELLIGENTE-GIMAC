package cm.gimac.des.participant.service;

import cm.gimac.des.participant.domain.*; // for static metamodels
import cm.gimac.des.participant.domain.LegalDocument;
import cm.gimac.des.participant.repository.LegalDocumentRepository;
import cm.gimac.des.participant.service.criteria.LegalDocumentCriteria;
import cm.gimac.des.participant.service.dto.LegalDocumentDTO;
import cm.gimac.des.participant.service.mapper.LegalDocumentMapper;
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
 * Service for executing complex queries for {@link LegalDocument} entities in the database.
 * The main input is a {@link LegalDocumentCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link LegalDocumentDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class LegalDocumentQueryService extends QueryService<LegalDocument> {

    private static final Logger LOG = LoggerFactory.getLogger(LegalDocumentQueryService.class);

    private final LegalDocumentRepository legalDocumentRepository;

    private final LegalDocumentMapper legalDocumentMapper;

    public LegalDocumentQueryService(LegalDocumentRepository legalDocumentRepository, LegalDocumentMapper legalDocumentMapper) {
        this.legalDocumentRepository = legalDocumentRepository;
        this.legalDocumentMapper = legalDocumentMapper;
    }

    /**
     * Return a {@link Page} of {@link LegalDocumentDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<LegalDocumentDTO> findByCriteria(LegalDocumentCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<LegalDocument> specification = createSpecification(criteria);
        return legalDocumentRepository.findAll(specification, page).map(legalDocumentMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(LegalDocumentCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<LegalDocument> specification = createSpecification(criteria);
        return legalDocumentRepository.count(specification);
    }

    /**
     * Function to convert {@link LegalDocumentCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<LegalDocument> createSpecification(LegalDocumentCriteria criteria) {
        Specification<LegalDocument> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), LegalDocument_.id),
                buildSpecification(criteria.getType(), LegalDocument_.type),
                buildStringSpecification(criteria.getReference(), LegalDocument_.reference),
                buildStringSpecification(criteria.getIssuingAuthority(), LegalDocument_.issuingAuthority),
                buildRangeSpecification(criteria.getIssuedOn(), LegalDocument_.issuedOn),
                buildRangeSpecification(criteria.getExpiresOn(), LegalDocument_.expiresOn),
                buildSpecification(criteria.getDocumentId(), LegalDocument_.documentId),
                buildRangeSpecification(criteria.getDocumentVersion(), LegalDocument_.documentVersion),
                buildStringSpecification(criteria.getDigest(), LegalDocument_.digest),
                buildSpecification(criteria.getSealed(), LegalDocument_.sealed),
                buildSpecification(criteria.getState(), LegalDocument_.state),
                buildSpecification(criteria.getPreviousDocumentId(), root ->
                    root.join(LegalDocument_.previousDocument, JoinType.LEFT).get(LegalDocument_.id)
                ),
                buildSpecification(criteria.getInstitutionId(), root ->
                    root.join(LegalDocument_.institution, JoinType.LEFT).get(Institution_.id)
                ),
                buildSpecification(criteria.getLegalDocumentId(), root ->
                    root.join(LegalDocument_.legalDocument, JoinType.LEFT).get(LegalDocument_.id)
                )
            );
        }
        return specification;
    }
}
