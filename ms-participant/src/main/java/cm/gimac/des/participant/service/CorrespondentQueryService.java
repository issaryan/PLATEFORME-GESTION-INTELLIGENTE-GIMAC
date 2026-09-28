package cm.gimac.des.participant.service;

import cm.gimac.des.participant.domain.*; // for static metamodels
import cm.gimac.des.participant.domain.Correspondent;
import cm.gimac.des.participant.repository.CorrespondentRepository;
import cm.gimac.des.participant.service.criteria.CorrespondentCriteria;
import cm.gimac.des.participant.service.dto.CorrespondentDTO;
import cm.gimac.des.participant.service.mapper.CorrespondentMapper;
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
 * Service for executing complex queries for {@link Correspondent} entities in the database.
 * The main input is a {@link CorrespondentCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link CorrespondentDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class CorrespondentQueryService extends QueryService<Correspondent> {

    private static final Logger LOG = LoggerFactory.getLogger(CorrespondentQueryService.class);

    private final CorrespondentRepository correspondentRepository;

    private final CorrespondentMapper correspondentMapper;

    public CorrespondentQueryService(CorrespondentRepository correspondentRepository, CorrespondentMapper correspondentMapper) {
        this.correspondentRepository = correspondentRepository;
        this.correspondentMapper = correspondentMapper;
    }

    /**
     * Return a {@link Page} of {@link CorrespondentDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<CorrespondentDTO> findByCriteria(CorrespondentCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Correspondent> specification = createSpecification(criteria);
        return correspondentRepository.findAll(specification, page).map(correspondentMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(CorrespondentCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Correspondent> specification = createSpecification(criteria);
        return correspondentRepository.count(specification);
    }

    /**
     * Function to convert {@link CorrespondentCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Correspondent> createSpecification(CorrespondentCriteria criteria) {
        Specification<Correspondent> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), Correspondent_.id),
                buildStringSpecification(criteria.getFullName(), Correspondent_.fullName),
                buildSpecification(criteria.getDuty(), Correspondent_.duty),
                buildStringSpecification(criteria.getChannels(), Correspondent_.channels),
                buildStringSpecification(criteria.getEmail(), Correspondent_.email),
                buildStringSpecification(criteria.getPhone(), Correspondent_.phone),
                buildStringSpecification(criteria.getPublicKeyRef(), Correspondent_.publicKeyRef),
                buildSpecification(criteria.getReachability(), Correspondent_.reachability),
                buildRangeSpecification(criteria.getLastReachabilityCheckAt(), Correspondent_.lastReachabilityCheckAt),
                buildSpecification(criteria.getOnCall247(), Correspondent_.onCall247),
                buildSpecification(criteria.getUserId(), Correspondent_.userId),
                buildSpecification(criteria.getActive(), Correspondent_.active),
                buildSpecification(criteria.getProposedBy(), Correspondent_.proposedBy),
                buildSpecification(criteria.getValidatedBy(), Correspondent_.validatedBy),
                buildRangeSpecification(criteria.getValidatedAt(), Correspondent_.validatedAt),
                buildSpecification(criteria.getInstitutionId(), root ->
                    root.join(Correspondent_.institution, JoinType.LEFT).get(Institution_.id)
                )
            );
        }
        return specification;
    }
}
