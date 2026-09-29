package cm.gimac.des.participant.service;

import cm.gimac.des.participant.domain.*; // for static metamodels
import cm.gimac.des.participant.domain.Deadline;
import cm.gimac.des.participant.repository.DeadlineRepository;
import cm.gimac.des.participant.service.criteria.DeadlineCriteria;
import cm.gimac.des.participant.service.dto.DeadlineDTO;
import cm.gimac.des.participant.service.mapper.DeadlineMapper;
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
 * Service for executing complex queries for {@link Deadline} entities in the database.
 * The main input is a {@link DeadlineCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link DeadlineDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class DeadlineQueryService extends QueryService<Deadline> {

    private static final Logger LOG = LoggerFactory.getLogger(DeadlineQueryService.class);

    private final DeadlineRepository deadlineRepository;

    private final DeadlineMapper deadlineMapper;

    public DeadlineQueryService(DeadlineRepository deadlineRepository, DeadlineMapper deadlineMapper) {
        this.deadlineRepository = deadlineRepository;
        this.deadlineMapper = deadlineMapper;
    }

    /**
     * Return a {@link Page} of {@link DeadlineDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<DeadlineDTO> findByCriteria(DeadlineCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Deadline> specification = createSpecification(criteria);
        return deadlineRepository.findAll(specification, page).map(deadlineMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(DeadlineCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Deadline> specification = createSpecification(criteria);
        return deadlineRepository.count(specification);
    }

    /**
     * Function to convert {@link DeadlineCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Deadline> createSpecification(DeadlineCriteria criteria) {
        Specification<Deadline> specification = Specification.unrestricted();
        specification = specification.and((root, query, builder) -> {
            if (Long.class != query.getResultType()) {
                root.fetch(Deadline_.institution, JoinType.LEFT);
                root.fetch(Deadline_.legalDocument, JoinType.LEFT);
                root.fetch(Deadline_.convention, JoinType.LEFT);
            }
            return null;
        });
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = specification.and(
                Specification.allOf(
                    Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : Specification.unrestricted(),
                    buildSpecification(criteria.getId(), Deadline_.id),
                    buildSpecification(criteria.getThreshold(), Deadline_.threshold),
                    buildRangeSpecification(criteria.getDueOn(), Deadline_.dueOn),
                    buildSpecification(criteria.getState(), Deadline_.state),
                    buildStringSpecification(criteria.getTriggeredAction(), Deadline_.triggeredAction),
                    buildRangeSpecification(criteria.getTriggeredAt(), Deadline_.triggeredAt),
                    buildSpecification(criteria.getAcknowledgementRequired(), Deadline_.acknowledgementRequired),
                    buildSpecification(criteria.getNotificationId(), Deadline_.notificationId),
                    buildRangeSpecification(criteria.getAcknowledgedAt(), Deadline_.acknowledgedAt),
                    buildSpecification(criteria.getInstitutionId(), root ->
                        root.join(Deadline_.institution, JoinType.LEFT).get(Institution_.id)
                    ),
                    buildSpecification(criteria.getLegalDocumentId(), root ->
                        root.join(Deadline_.legalDocument, JoinType.LEFT).get(LegalDocument_.id)
                    ),
                    buildSpecification(criteria.getConventionId(), root ->
                        root.join(Deadline_.convention, JoinType.LEFT).get(Convention_.id)
                    )
                )
            );
        }
        return specification;
    }
}
