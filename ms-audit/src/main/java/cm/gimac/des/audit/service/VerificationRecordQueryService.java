package cm.gimac.des.audit.service;

import cm.gimac.des.audit.domain.*; // for static metamodels
import cm.gimac.des.audit.domain.VerificationRecord;
import cm.gimac.des.audit.repository.VerificationRecordRepository;
import cm.gimac.des.audit.service.criteria.VerificationRecordCriteria;
import cm.gimac.des.audit.service.dto.VerificationRecordDTO;
import cm.gimac.des.audit.service.mapper.VerificationRecordMapper;
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
 * Service for executing complex queries for {@link VerificationRecord} entities in the database.
 * The main input is a {@link VerificationRecordCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link VerificationRecordDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class VerificationRecordQueryService extends QueryService<VerificationRecord> {

    private static final Logger LOG = LoggerFactory.getLogger(VerificationRecordQueryService.class);

    private final VerificationRecordRepository verificationRecordRepository;

    private final VerificationRecordMapper verificationRecordMapper;

    public VerificationRecordQueryService(
        VerificationRecordRepository verificationRecordRepository,
        VerificationRecordMapper verificationRecordMapper
    ) {
        this.verificationRecordRepository = verificationRecordRepository;
        this.verificationRecordMapper = verificationRecordMapper;
    }

    /**
     * Return a {@link Page} of {@link VerificationRecordDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<VerificationRecordDTO> findByCriteria(VerificationRecordCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<VerificationRecord> specification = createSpecification(criteria);
        return verificationRecordRepository.findAll(specification, page).map(verificationRecordMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(VerificationRecordCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<VerificationRecord> specification = createSpecification(criteria);
        return verificationRecordRepository.count(specification);
    }

    /**
     * Function to convert {@link VerificationRecordCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<VerificationRecord> createSpecification(VerificationRecordCriteria criteria) {
        Specification<VerificationRecord> specification = Specification.unrestricted();
        specification = specification.and((root, query, builder) -> {
            if (Long.class != query.getResultType()) {
                root.fetch(VerificationRecord_.proof, JoinType.LEFT);
            }
            return null;
        });
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = specification.and(
                Specification.allOf(
                    Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : Specification.unrestricted(),
                    buildSpecification(criteria.getId(), VerificationRecord_.id),
                    buildSpecification(criteria.getVerifierId(), VerificationRecord_.verifierId),
                    buildRangeSpecification(criteria.getVerifiedAt(), VerificationRecord_.verifiedAt),
                    buildStringSpecification(criteria.getExpectedRoot(), VerificationRecord_.expectedRoot),
                    buildStringSpecification(criteria.getRecomputedRoot(), VerificationRecord_.recomputedRoot),
                    buildSpecification(criteria.getTokenValid(), VerificationRecord_.tokenValid),
                    buildSpecification(criteria.getResult(), VerificationRecord_.result),
                    buildStringSpecification(criteria.getComment(), VerificationRecord_.comment),
                    buildSpecification(criteria.getProofId(), root ->
                        root.join(VerificationRecord_.proof, JoinType.LEFT).get(InclusionProof_.id)
                    )
                )
            );
        }
        return specification;
    }
}
