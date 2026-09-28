package cm.gimac.des.certification.service;

import cm.gimac.des.certification.domain.*; // for static metamodels
import cm.gimac.des.certification.domain.EvidenceItem;
import cm.gimac.des.certification.repository.EvidenceItemRepository;
import cm.gimac.des.certification.service.criteria.EvidenceItemCriteria;
import cm.gimac.des.certification.service.dto.EvidenceItemDTO;
import cm.gimac.des.certification.service.mapper.EvidenceItemMapper;
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
 * Service for executing complex queries for {@link EvidenceItem} entities in the database.
 * The main input is a {@link EvidenceItemCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link EvidenceItemDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class EvidenceItemQueryService extends QueryService<EvidenceItem> {

    private static final Logger LOG = LoggerFactory.getLogger(EvidenceItemQueryService.class);

    private final EvidenceItemRepository evidenceItemRepository;

    private final EvidenceItemMapper evidenceItemMapper;

    public EvidenceItemQueryService(EvidenceItemRepository evidenceItemRepository, EvidenceItemMapper evidenceItemMapper) {
        this.evidenceItemRepository = evidenceItemRepository;
        this.evidenceItemMapper = evidenceItemMapper;
    }

    /**
     * Return a {@link Page} of {@link EvidenceItemDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<EvidenceItemDTO> findByCriteria(EvidenceItemCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<EvidenceItem> specification = createSpecification(criteria);
        return evidenceItemRepository.findAll(specification, page).map(evidenceItemMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(EvidenceItemCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<EvidenceItem> specification = createSpecification(criteria);
        return evidenceItemRepository.count(specification);
    }

    /**
     * Function to convert {@link EvidenceItemCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<EvidenceItem> createSpecification(EvidenceItemCriteria criteria) {
        Specification<EvidenceItem> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), EvidenceItem_.id),
                buildSpecification(criteria.getDocumentId(), EvidenceItem_.documentId),
                buildSpecification(criteria.getKind(), EvidenceItem_.kind),
                buildStringSpecification(criteria.getDigest(), EvidenceItem_.digest),
                buildRangeSpecification(criteria.getSealedAt(), EvidenceItem_.sealedAt),
                buildSpecification(criteria.getSealConfirmed(), EvidenceItem_.sealConfirmed),
                buildRangeSpecification(criteria.getLastVerifiedAt(), EvidenceItem_.lastVerifiedAt),
                buildSpecification(criteria.getAttachedBy(), EvidenceItem_.attachedBy),
                buildRangeSpecification(criteria.getAttachedAt(), EvidenceItem_.attachedAt),
                buildSpecification(criteria.getFileId(), root -> root.join(EvidenceItem_.file, JoinType.LEFT).get(CommissioningFile_.id)),
                buildSpecification(criteria.getCommissioningFileId(), root ->
                    root.join(EvidenceItem_.commissioningFile, JoinType.LEFT).get(CommissioningFile_.id)
                )
            );
        }
        return specification;
    }
}
