package cm.gimac.des.participant.service;

import cm.gimac.des.participant.domain.*; // for static metamodels
import cm.gimac.des.participant.domain.Interaction;
import cm.gimac.des.participant.repository.InteractionRepository;
import cm.gimac.des.participant.service.criteria.InteractionCriteria;
import cm.gimac.des.participant.service.dto.InteractionDTO;
import cm.gimac.des.participant.service.mapper.InteractionMapper;
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
 * Service for executing complex queries for {@link Interaction} entities in the database.
 * The main input is a {@link InteractionCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link InteractionDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class InteractionQueryService extends QueryService<Interaction> {

    private static final Logger LOG = LoggerFactory.getLogger(InteractionQueryService.class);

    private final InteractionRepository interactionRepository;

    private final InteractionMapper interactionMapper;

    public InteractionQueryService(InteractionRepository interactionRepository, InteractionMapper interactionMapper) {
        this.interactionRepository = interactionRepository;
        this.interactionMapper = interactionMapper;
    }

    /**
     * Return a {@link Page} of {@link InteractionDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<InteractionDTO> findByCriteria(InteractionCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Interaction> specification = createSpecification(criteria);
        return interactionRepository.findAll(specification, page).map(interactionMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(InteractionCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Interaction> specification = createSpecification(criteria);
        return interactionRepository.count(specification);
    }

    /**
     * Function to convert {@link InteractionCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Interaction> createSpecification(InteractionCriteria criteria) {
        Specification<Interaction> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), Interaction_.id),
                buildSpecification(criteria.getNature(), Interaction_.nature),
                buildStringSpecification(criteria.getSummary(), Interaction_.summary),
                buildRangeSpecification(criteria.getOccurredAt(), Interaction_.occurredAt),
                buildSpecification(criteria.getAuthorId(), Interaction_.authorId),
                buildStringSpecification(criteria.getReference(), Interaction_.reference),
                buildSpecification(criteria.getNotificationId(), Interaction_.notificationId),
                buildSpecification(criteria.getSourceEventId(), Interaction_.sourceEventId),
                buildSpecification(criteria.getInstitutionId(), root ->
                    root.join(Interaction_.institution, JoinType.LEFT).get(Institution_.id)
                )
            );
        }
        return specification;
    }
}
