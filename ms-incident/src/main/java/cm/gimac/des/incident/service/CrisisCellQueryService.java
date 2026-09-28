package cm.gimac.des.incident.service;

import cm.gimac.des.incident.domain.*; // for static metamodels
import cm.gimac.des.incident.domain.CrisisCell;
import cm.gimac.des.incident.repository.CrisisCellRepository;
import cm.gimac.des.incident.service.criteria.CrisisCellCriteria;
import cm.gimac.des.incident.service.dto.CrisisCellDTO;
import cm.gimac.des.incident.service.mapper.CrisisCellMapper;
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
 * Service for executing complex queries for {@link CrisisCell} entities in the database.
 * The main input is a {@link CrisisCellCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link CrisisCellDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class CrisisCellQueryService extends QueryService<CrisisCell> {

    private static final Logger LOG = LoggerFactory.getLogger(CrisisCellQueryService.class);

    private final CrisisCellRepository crisisCellRepository;

    private final CrisisCellMapper crisisCellMapper;

    public CrisisCellQueryService(CrisisCellRepository crisisCellRepository, CrisisCellMapper crisisCellMapper) {
        this.crisisCellRepository = crisisCellRepository;
        this.crisisCellMapper = crisisCellMapper;
    }

    /**
     * Return a {@link Page} of {@link CrisisCellDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<CrisisCellDTO> findByCriteria(CrisisCellCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<CrisisCell> specification = createSpecification(criteria);
        return crisisCellRepository.findAll(specification, page).map(crisisCellMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(CrisisCellCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<CrisisCell> specification = createSpecification(criteria);
        return crisisCellRepository.count(specification);
    }

    /**
     * Function to convert {@link CrisisCellCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<CrisisCell> createSpecification(CrisisCellCriteria criteria) {
        Specification<CrisisCell> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), CrisisCell_.id),
                buildStringSpecification(criteria.getReference(), CrisisCell_.reference),
                buildSpecification(criteria.getLevel(), CrisisCell_.level),
                buildSpecification(criteria.getState(), CrisisCell_.state),
                buildSpecification(criteria.getAutomaticActivation(), CrisisCell_.automaticActivation),
                buildStringSpecification(criteria.getActivationReason(), CrisisCell_.activationReason),
                buildSpecification(criteria.getConvenedBy(), CrisisCell_.convenedBy),
                buildRangeSpecification(criteria.getConvenedAt(), CrisisCell_.convenedAt),
                buildRangeSpecification(criteria.getQuorumDeadline(), CrisisCell_.quorumDeadline),
                buildRangeSpecification(criteria.getQuorumThreshold(), CrisisCell_.quorumThreshold),
                buildRangeSpecification(criteria.getConstitutedAt(), CrisisCell_.constitutedAt),
                buildStringSpecification(criteria.getRoomRef(), CrisisCell_.roomRef),
                buildRangeSpecification(criteria.getPostIncidentReportDueAt(), CrisisCell_.postIncidentReportDueAt),
                buildStringSpecification(criteria.getPostIncidentReportRef(), CrisisCell_.postIncidentReportRef),
                buildStringSpecification(criteria.getPostIncidentReportDigest(), CrisisCell_.postIncidentReportDigest),
                buildSpecification(criteria.getPostIncidentReportSignedBy(), CrisisCell_.postIncidentReportSignedBy),
                buildRangeSpecification(criteria.getPostIncidentReportSignedAt(), CrisisCell_.postIncidentReportSignedAt),
                buildSpecification(criteria.getClosedBy(), CrisisCell_.closedBy),
                buildRangeSpecification(criteria.getClosedAt(), CrisisCell_.closedAt),
                buildStringSpecification(criteria.getArchiveRef(), CrisisCell_.archiveRef),
                buildRangeSpecification(criteria.getArchivedAt(), CrisisCell_.archivedAt),
                buildSpecification(criteria.getIncidentId(), root -> root.join(CrisisCell_.incident, JoinType.LEFT).get(Incident_.id))
            );
        }
        return specification;
    }
}
