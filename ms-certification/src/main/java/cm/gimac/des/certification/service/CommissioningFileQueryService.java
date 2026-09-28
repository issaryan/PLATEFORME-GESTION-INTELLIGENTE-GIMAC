package cm.gimac.des.certification.service;

import cm.gimac.des.certification.domain.*; // for static metamodels
import cm.gimac.des.certification.domain.CommissioningFile;
import cm.gimac.des.certification.repository.CommissioningFileRepository;
import cm.gimac.des.certification.service.criteria.CommissioningFileCriteria;
import cm.gimac.des.certification.service.dto.CommissioningFileDTO;
import cm.gimac.des.certification.service.mapper.CommissioningFileMapper;
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
 * Service for executing complex queries for {@link CommissioningFile} entities in the database.
 * The main input is a {@link CommissioningFileCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link CommissioningFileDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class CommissioningFileQueryService extends QueryService<CommissioningFile> {

    private static final Logger LOG = LoggerFactory.getLogger(CommissioningFileQueryService.class);

    private final CommissioningFileRepository commissioningFileRepository;

    private final CommissioningFileMapper commissioningFileMapper;

    public CommissioningFileQueryService(
        CommissioningFileRepository commissioningFileRepository,
        CommissioningFileMapper commissioningFileMapper
    ) {
        this.commissioningFileRepository = commissioningFileRepository;
        this.commissioningFileMapper = commissioningFileMapper;
    }

    /**
     * Return a {@link Page} of {@link CommissioningFileDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<CommissioningFileDTO> findByCriteria(CommissioningFileCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<CommissioningFile> specification = createSpecification(criteria);
        return commissioningFileRepository.findAll(specification, page).map(commissioningFileMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(CommissioningFileCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<CommissioningFile> specification = createSpecification(criteria);
        return commissioningFileRepository.count(specification);
    }

    /**
     * Function to convert {@link CommissioningFileCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<CommissioningFile> createSpecification(CommissioningFileCriteria criteria) {
        Specification<CommissioningFile> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), CommissioningFile_.id),
                buildStringSpecification(criteria.getFileCode(), CommissioningFile_.fileCode),
                buildSpecification(criteria.getLiaisonId(), CommissioningFile_.liaisonId),
                buildSpecification(criteria.getInstitutionId(), CommissioningFile_.institutionId),
                buildSpecification(criteria.getProjectLeadId(), CommissioningFile_.projectLeadId),
                buildSpecification(criteria.getState(), CommissioningFile_.state),
                buildStringSpecification(criteria.getBasisDigest(), CommissioningFile_.basisDigest),
                buildRangeSpecification(criteria.getBasisSealedAt(), CommissioningFile_.basisSealedAt),
                buildRangeSpecification(criteria.getOpenedAt(), CommissioningFile_.openedAt),
                buildStringSpecification(criteria.getCorrectionReason(), CommissioningFile_.correctionReason),
                buildStringSpecification(criteria.getManifestDigest(), CommissioningFile_.manifestDigest),
                buildRangeSpecification(criteria.getSealedAt(), CommissioningFile_.sealedAt),
                buildRangeSpecification(criteria.getGoLiveAuthorizedAt(), CommissioningFile_.goLiveAuthorizedAt),
                buildSpecification(criteria.getChangeRecordId(), root ->
                    root.join(CommissioningFile_.changeRecord, JoinType.LEFT).get(ChangeRecord_.id)
                ),
                buildSpecification(criteria.getAttestationId(), root ->
                    root.join(CommissioningFile_.attestation, JoinType.LEFT).get(CertificationAttestation_.id)
                ),
                buildSpecification(criteria.getCommissioningReportId(), root ->
                    root.join(CommissioningFile_.commissioningReport, JoinType.LEFT).get(EvidenceItem_.id)
                ),
                buildSpecification(criteria.getQuitusId(), root -> root.join(CommissioningFile_.quituses, JoinType.LEFT).get(Quitus_.id)),
                buildSpecification(criteria.getPiecesId(), root -> root.join(CommissioningFile_.pieces, JoinType.LEFT).get(EvidenceItem_.id)
                )
            );
        }
        return specification;
    }
}
