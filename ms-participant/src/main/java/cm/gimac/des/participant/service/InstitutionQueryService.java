package cm.gimac.des.participant.service;

import cm.gimac.des.participant.domain.*; // for static metamodels
import cm.gimac.des.participant.domain.Institution;
import cm.gimac.des.participant.repository.InstitutionRepository;
import cm.gimac.des.participant.service.criteria.InstitutionCriteria;
import cm.gimac.des.participant.service.dto.InstitutionDTO;
import cm.gimac.des.participant.service.mapper.InstitutionMapper;
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
 * Service for executing complex queries for {@link Institution} entities in the database.
 * The main input is a {@link InstitutionCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link InstitutionDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class InstitutionQueryService extends QueryService<Institution> {

    private static final Logger LOG = LoggerFactory.getLogger(InstitutionQueryService.class);

    private final InstitutionRepository institutionRepository;

    private final InstitutionMapper institutionMapper;

    public InstitutionQueryService(InstitutionRepository institutionRepository, InstitutionMapper institutionMapper) {
        this.institutionRepository = institutionRepository;
        this.institutionMapper = institutionMapper;
    }

    /**
     * Return a {@link Page} of {@link InstitutionDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<InstitutionDTO> findByCriteria(InstitutionCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Institution> specification = createSpecification(criteria);
        return institutionRepository.findAll(specification, page).map(institutionMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(InstitutionCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Institution> specification = createSpecification(criteria);
        return institutionRepository.count(specification);
    }

    /**
     * Function to convert {@link InstitutionCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Institution> createSpecification(InstitutionCriteria criteria) {
        Specification<Institution> specification = Specification.unrestricted();
        specification = specification.and((root, query, builder) -> {
            if (Long.class != query.getResultType()) {
                root.fetch(Institution_.settlementBank, JoinType.LEFT);
                root.fetch(Institution_.sponsorBank, JoinType.LEFT);
            }
            return null;
        });
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = specification.and(
                Specification.allOf(
                    Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : Specification.unrestricted(),
                    buildSpecification(criteria.getId(), Institution_.id),
                    buildStringSpecification(criteria.getName(), Institution_.name),
                    buildStringSpecification(criteria.getAcronym(), Institution_.acronym),
                    buildStringSpecification(criteria.getParticipantCode(), Institution_.participantCode),
                    buildStringSpecification(criteria.getIdentificationCode(), Institution_.identificationCode),
                    buildStringSpecification(criteria.getBeacBankCode(), Institution_.beacBankCode),
                    buildStringSpecification(criteria.getCountry(), Institution_.country),
                    buildSpecification(criteria.getCategory(), Institution_.category),
                    buildSpecification(criteria.getMembershipMode(), Institution_.membershipMode),
                    buildStringSpecification(criteria.getSettlementAccountRef(), Institution_.settlementAccountRef),
                    buildStringSpecification(criteria.getRingFencedAccountRef(), Institution_.ringFencedAccountRef),
                    buildRangeSpecification(criteria.getJoinedOn(), Institution_.joinedOn),
                    buildSpecification(criteria.getStatus(), Institution_.status),
                    buildSpecification(criteria.getComplianceState(), Institution_.complianceState),
                    buildRangeSpecification(criteria.getAttestationIssuedAt(), Institution_.attestationIssuedAt),
                    buildRangeSpecification(criteria.getGoLiveAuthorizedAt(), Institution_.goLiveAuthorizedAt),
                    buildRangeSpecification(criteria.getOpenLinkCount(), Institution_.openLinkCount),
                    buildRangeSpecification(criteria.getOpenSuspenseCount(), Institution_.openSuspenseCount),
                    buildRangeSpecification(criteria.getCreatedAt(), Institution_.createdAt),
                    buildRangeSpecification(criteria.getContactedOn(), Institution_.contactedOn),
                    buildStringSpecification(criteria.getContactOrigin(), Institution_.contactOrigin),
                    buildStringSpecification(criteria.getIntendedProject(), Institution_.intendedProject),
                    buildSpecification(criteria.getDdrmOfficerId(), Institution_.ddrmOfficerId),
                    buildStringSpecification(criteria.getDdrmOfficerName(), Institution_.ddrmOfficerName),
                    buildRangeSpecification(criteria.getAdmittedAt(), Institution_.admittedAt),
                    buildSpecification(criteria.getAdmittedById(), Institution_.admittedById),
                    buildStringSpecification(criteria.getClosureReason(), Institution_.closureReason),
                    buildRangeSpecification(criteria.getClosedAt(), Institution_.closedAt),
                    buildSpecification(criteria.getSettlementBankId(), root ->
                        root.join(Institution_.settlementBank, JoinType.LEFT).get(Institution_.id)
                    ),
                    buildSpecification(criteria.getSponsorBankId(), root ->
                        root.join(Institution_.sponsorBank, JoinType.LEFT).get(Institution_.id)
                    )
                )
            );
        }
        return specification;
    }
}
