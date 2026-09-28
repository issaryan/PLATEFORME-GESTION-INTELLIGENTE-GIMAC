package cm.gimac.des.collaboration.service;

import cm.gimac.des.collaboration.domain.*; // for static metamodels
import cm.gimac.des.collaboration.domain.Conference;
import cm.gimac.des.collaboration.repository.ConferenceRepository;
import cm.gimac.des.collaboration.service.criteria.ConferenceCriteria;
import cm.gimac.des.collaboration.service.dto.ConferenceDTO;
import cm.gimac.des.collaboration.service.mapper.ConferenceMapper;
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
 * Service for executing complex queries for {@link Conference} entities in the database.
 * The main input is a {@link ConferenceCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link ConferenceDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class ConferenceQueryService extends QueryService<Conference> {

    private static final Logger LOG = LoggerFactory.getLogger(ConferenceQueryService.class);

    private final ConferenceRepository conferenceRepository;

    private final ConferenceMapper conferenceMapper;

    public ConferenceQueryService(ConferenceRepository conferenceRepository, ConferenceMapper conferenceMapper) {
        this.conferenceRepository = conferenceRepository;
        this.conferenceMapper = conferenceMapper;
    }

    /**
     * Return a {@link Page} of {@link ConferenceDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<ConferenceDTO> findByCriteria(ConferenceCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Conference> specification = createSpecification(criteria);
        return conferenceRepository.findAll(specification, page).map(conferenceMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(ConferenceCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Conference> specification = createSpecification(criteria);
        return conferenceRepository.count(specification);
    }

    /**
     * Function to convert {@link ConferenceCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Conference> createSpecification(ConferenceCriteria criteria) {
        Specification<Conference> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), Conference_.id),
                buildStringSpecification(criteria.getSfuRoom(), Conference_.sfuRoom),
                buildRangeSpecification(criteria.getStartedAt(), Conference_.startedAt),
                buildRangeSpecification(criteria.getEndedAt(), Conference_.endedAt),
                buildSpecification(criteria.getSframeRequired(), Conference_.sframeRequired),
                buildSpecification(criteria.getScreenShare(), Conference_.screenShare),
                buildSpecification(criteria.getRecording(), Conference_.recording),
                buildRangeSpecification(criteria.getKeyEpoch(), Conference_.keyEpoch),
                buildSpecification(criteria.getRoomId(), root -> root.join(Conference_.room, JoinType.LEFT).get(Room_.id))
            );
        }
        return specification;
    }
}
