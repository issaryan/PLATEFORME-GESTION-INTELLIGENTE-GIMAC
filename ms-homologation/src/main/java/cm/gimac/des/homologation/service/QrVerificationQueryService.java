package cm.gimac.des.homologation.service;

import cm.gimac.des.homologation.domain.*; // for static metamodels
import cm.gimac.des.homologation.domain.QrVerification;
import cm.gimac.des.homologation.repository.QrVerificationRepository;
import cm.gimac.des.homologation.service.criteria.QrVerificationCriteria;
import cm.gimac.des.homologation.service.dto.QrVerificationDTO;
import cm.gimac.des.homologation.service.mapper.QrVerificationMapper;
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
 * Service for executing complex queries for {@link QrVerification} entities in the database.
 * The main input is a {@link QrVerificationCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link QrVerificationDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class QrVerificationQueryService extends QueryService<QrVerification> {

    private static final Logger LOG = LoggerFactory.getLogger(QrVerificationQueryService.class);

    private final QrVerificationRepository qrVerificationRepository;

    private final QrVerificationMapper qrVerificationMapper;

    public QrVerificationQueryService(QrVerificationRepository qrVerificationRepository, QrVerificationMapper qrVerificationMapper) {
        this.qrVerificationRepository = qrVerificationRepository;
        this.qrVerificationMapper = qrVerificationMapper;
    }

    /**
     * Return a {@link Page} of {@link QrVerificationDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<QrVerificationDTO> findByCriteria(QrVerificationCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<QrVerification> specification = createSpecification(criteria);
        return qrVerificationRepository.findAll(specification, page).map(qrVerificationMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(QrVerificationCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<QrVerification> specification = createSpecification(criteria);
        return qrVerificationRepository.count(specification);
    }

    /**
     * Function to convert {@link QrVerificationCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<QrVerification> createSpecification(QrVerificationCriteria criteria) {
        Specification<QrVerification> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), QrVerification_.id),
                buildSpecification(criteria.getForm(), QrVerification_.form),
                buildSpecification(criteria.getOutcome(), QrVerification_.outcome),
                buildRangeSpecification(criteria.getVerifiedOn(), QrVerification_.verifiedOn),
                buildSpecification(criteria.getVerifierId(), QrVerification_.verifierId),
                buildSpecification(criteria.getEvidenceDocumentId(), QrVerification_.evidenceDocumentId),
                buildStringSpecification(criteria.getEvidenceDigest(), QrVerification_.evidenceDigest),
                buildRangeSpecification(criteria.getEvidenceSealedAt(), QrVerification_.evidenceSealedAt),
                buildSpecification(criteria.getLiaisonId(), root -> root.join(QrVerification_.liaison, JoinType.LEFT).get(Liaison_.id)),
                buildSpecification(criteria.getOperationId(), root -> root.join(QrVerification_.operation, JoinType.LEFT).get(Operation_.id)
                )
            );
        }
        return specification;
    }
}
