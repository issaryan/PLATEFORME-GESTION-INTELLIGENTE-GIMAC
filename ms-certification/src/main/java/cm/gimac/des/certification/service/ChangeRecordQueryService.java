package cm.gimac.des.certification.service;

import cm.gimac.des.certification.domain.*; // for static metamodels
import cm.gimac.des.certification.domain.ChangeRecord;
import cm.gimac.des.certification.repository.ChangeRecordRepository;
import cm.gimac.des.certification.service.criteria.ChangeRecordCriteria;
import cm.gimac.des.certification.service.dto.ChangeRecordDTO;
import cm.gimac.des.certification.service.mapper.ChangeRecordMapper;
import jakarta.persistence.criteria.JoinType;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Service for executing complex queries for {@link ChangeRecord} entities in the database.
 * The main input is a {@link ChangeRecordCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link List} of {@link ChangeRecordDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class ChangeRecordQueryService extends QueryService<ChangeRecord> {

    private static final Logger LOG = LoggerFactory.getLogger(ChangeRecordQueryService.class);

    private final ChangeRecordRepository changeRecordRepository;

    private final ChangeRecordMapper changeRecordMapper;

    public ChangeRecordQueryService(ChangeRecordRepository changeRecordRepository, ChangeRecordMapper changeRecordMapper) {
        this.changeRecordRepository = changeRecordRepository;
        this.changeRecordMapper = changeRecordMapper;
    }

    /**
     * Return a {@link List} of {@link ChangeRecordDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public List<ChangeRecordDTO> findByCriteria(ChangeRecordCriteria criteria) {
        LOG.debug("find by criteria : {}", criteria);
        final Specification<ChangeRecord> specification = createSpecification(criteria);
        return changeRecordMapper.toDto(changeRecordRepository.findAll(specification));
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(ChangeRecordCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<ChangeRecord> specification = createSpecification(criteria);
        return changeRecordRepository.count(specification);
    }

    /**
     * Function to convert {@link ChangeRecordCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<ChangeRecord> createSpecification(ChangeRecordCriteria criteria) {
        Specification<ChangeRecord> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), ChangeRecord_.id),
                buildStringSpecification(criteria.getExternalRef(), ChangeRecord_.externalRef),
                buildSpecification(criteria.getApproval(), ChangeRecord_.approval),
                buildRangeSpecification(criteria.getApprovedAt(), ChangeRecord_.approvedAt),
                buildRangeSpecification(criteria.getWindowStart(), ChangeRecord_.windowStart),
                buildRangeSpecification(criteria.getWindowEnd(), ChangeRecord_.windowEnd),
                buildRangeSpecification(criteria.getReadAt(), ChangeRecord_.readAt),
                buildStringSpecification(criteria.getContentDigest(), ChangeRecord_.contentDigest),
                buildSpecification(criteria.getReferencedBy(), ChangeRecord_.referencedBy),
                buildSpecification(criteria.getCommissioningFileId(), root ->
                    root.join(ChangeRecord_.commissioningFile, JoinType.LEFT).get(CommissioningFile_.id)
                )
            );
        }
        return specification;
    }
}
