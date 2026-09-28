package cm.gimac.des.participant.service;

import cm.gimac.des.participant.domain.*; // for static metamodels
import cm.gimac.des.participant.domain.EscalationContact;
import cm.gimac.des.participant.repository.EscalationContactRepository;
import cm.gimac.des.participant.service.criteria.EscalationContactCriteria;
import cm.gimac.des.participant.service.dto.EscalationContactDTO;
import cm.gimac.des.participant.service.mapper.EscalationContactMapper;
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
 * Service for executing complex queries for {@link EscalationContact} entities in the database.
 * The main input is a {@link EscalationContactCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link EscalationContactDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class EscalationContactQueryService extends QueryService<EscalationContact> {

    private static final Logger LOG = LoggerFactory.getLogger(EscalationContactQueryService.class);

    private final EscalationContactRepository escalationContactRepository;

    private final EscalationContactMapper escalationContactMapper;

    public EscalationContactQueryService(
        EscalationContactRepository escalationContactRepository,
        EscalationContactMapper escalationContactMapper
    ) {
        this.escalationContactRepository = escalationContactRepository;
        this.escalationContactMapper = escalationContactMapper;
    }

    /**
     * Return a {@link Page} of {@link EscalationContactDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<EscalationContactDTO> findByCriteria(EscalationContactCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<EscalationContact> specification = createSpecification(criteria);
        return escalationContactRepository.findAll(specification, page).map(escalationContactMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(EscalationContactCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<EscalationContact> specification = createSpecification(criteria);
        return escalationContactRepository.count(specification);
    }

    /**
     * Function to convert {@link EscalationContactCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<EscalationContact> createSpecification(EscalationContactCriteria criteria) {
        Specification<EscalationContact> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), EscalationContact_.id),
                buildRangeSpecification(criteria.getSeverity(), EscalationContact_.severity),
                buildRangeSpecification(criteria.getEscalationRank(), EscalationContact_.escalationRank),
                buildRangeSpecification(criteria.getAckDelay(), EscalationContact_.ackDelay),
                buildSpecification(criteria.getActive(), EscalationContact_.active),
                buildSpecification(criteria.getInstitutionId(), root ->
                    root.join(EscalationContact_.institution, JoinType.LEFT).get(Institution_.id)
                ),
                buildSpecification(criteria.getCorrespondentId(), root ->
                    root.join(EscalationContact_.correspondent, JoinType.LEFT).get(Correspondent_.id)
                ),
                buildSpecification(criteria.getDeputyId(), root ->
                    root.join(EscalationContact_.deputy, JoinType.LEFT).get(Correspondent_.id)
                )
            );
        }
        return specification;
    }
}
