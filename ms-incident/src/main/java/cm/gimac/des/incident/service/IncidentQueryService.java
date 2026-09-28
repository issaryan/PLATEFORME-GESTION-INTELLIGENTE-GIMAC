package cm.gimac.des.incident.service;

import cm.gimac.des.incident.domain.*; // for static metamodels
import cm.gimac.des.incident.domain.Incident;
import cm.gimac.des.incident.repository.IncidentRepository;
import cm.gimac.des.incident.service.criteria.IncidentCriteria;
import cm.gimac.des.incident.service.dto.IncidentDTO;
import cm.gimac.des.incident.service.mapper.IncidentMapper;
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
 * Service for executing complex queries for {@link Incident} entities in the database.
 * The main input is a {@link IncidentCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link IncidentDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class IncidentQueryService extends QueryService<Incident> {

    private static final Logger LOG = LoggerFactory.getLogger(IncidentQueryService.class);

    private final IncidentRepository incidentRepository;

    private final IncidentMapper incidentMapper;

    public IncidentQueryService(IncidentRepository incidentRepository, IncidentMapper incidentMapper) {
        this.incidentRepository = incidentRepository;
        this.incidentMapper = incidentMapper;
    }

    /**
     * Return a {@link Page} of {@link IncidentDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<IncidentDTO> findByCriteria(IncidentCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Incident> specification = createSpecification(criteria);
        return incidentRepository.findAll(specification, page).map(incidentMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(IncidentCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Incident> specification = createSpecification(criteria);
        return incidentRepository.count(specification);
    }

    /**
     * Function to convert {@link IncidentCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Incident> createSpecification(IncidentCriteria criteria) {
        Specification<Incident> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), Incident_.id),
                buildRangeSpecification(criteria.getImpact(), Incident_.impact),
                buildRangeSpecification(criteria.getUrgency(), Incident_.urgency),
                buildRangeSpecification(criteria.getScore(), Incident_.score),
                buildSpecification(criteria.getSeverity(), Incident_.severity),
                buildSpecification(criteria.getSecurityIncident(), Incident_.securityIncident),
                buildSpecification(criteria.getDataCompromiseSuspected(), Incident_.dataCompromiseSuspected),
                buildSpecification(criteria.getRegulatoryFlag(), Incident_.regulatoryFlag),
                buildRangeSpecification(criteria.getQualifiedAt(), Incident_.qualifiedAt),
                buildSpecification(criteria.getQualifiedBy(), Incident_.qualifiedBy),
                buildStringSpecification(criteria.getRootCause(), Incident_.rootCause),
                buildStringSpecification(criteria.getRootCauseCategory(), Incident_.rootCauseCategory),
                buildStringSpecification(criteria.getRootCauseProofRef(), Incident_.rootCauseProofRef),
                buildStringSpecification(criteria.getResolutionTest(), Incident_.resolutionTest),
                buildSpecification(criteria.getResolutionTestPassed(), Incident_.resolutionTestPassed),
                buildSpecification(criteria.getResolutionTestAuthorId(), Incident_.resolutionTestAuthorId),
                buildRangeSpecification(criteria.getResolutionTestAt(), Incident_.resolutionTestAt),
                buildStringSpecification(criteria.getResolutionTestProofRef(), Incident_.resolutionTestProofRef),
                buildStringSpecification(criteria.getLessonsLearnedRef(), Incident_.lessonsLearnedRef),
                buildStringSpecification(criteria.getLessonsLearnedWaiver(), Incident_.lessonsLearnedWaiver),
                buildRangeSpecification(criteria.getReportDueAt(), Incident_.reportDueAt),
                buildRangeSpecification(criteria.getReportGeneratedAt(), Incident_.reportGeneratedAt),
                buildStringSpecification(criteria.getReportRef(), Incident_.reportRef),
                buildStringSpecification(criteria.getReportDigest(), Incident_.reportDigest),
                buildSpecification(criteria.getComplianceVisaBy(), Incident_.complianceVisaBy),
                buildRangeSpecification(criteria.getComplianceVisaAt(), Incident_.complianceVisaAt),
                buildRangeSpecification(criteria.getCriteriaValidatedAt(), Incident_.criteriaValidatedAt),
                buildSpecification(criteria.getValidatorId(), Incident_.validatorId),
                buildRangeSpecification(criteria.getRawDuration(), Incident_.rawDuration),
                buildRangeSpecification(criteria.getFrozenDuration(), Incident_.frozenDuration),
                buildRangeSpecification(criteria.getImputableDelay(), Incident_.imputableDelay),
                buildRangeSpecification(criteria.getMttr(), Incident_.mttr),
                buildSpecification(criteria.getTakeChargeMet(), Incident_.takeChargeMet),
                buildSpecification(criteria.getRestorationMet(), Incident_.restorationMet),
                buildSpecification(criteria.getTicketId(), root -> root.join(Incident_.ticket, JoinType.LEFT).get(Ticket_.id)),
                buildSpecification(criteria.getMatrixId(), root -> root.join(Incident_.matrix, JoinType.LEFT).get(QualificationMatrix_.id))
            );
        }
        return specification;
    }
}
