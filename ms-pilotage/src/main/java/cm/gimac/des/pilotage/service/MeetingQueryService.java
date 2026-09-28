package cm.gimac.des.pilotage.service;

import cm.gimac.des.pilotage.domain.*; // for static metamodels
import cm.gimac.des.pilotage.domain.Meeting;
import cm.gimac.des.pilotage.repository.MeetingRepository;
import cm.gimac.des.pilotage.service.criteria.MeetingCriteria;
import cm.gimac.des.pilotage.service.dto.MeetingDTO;
import cm.gimac.des.pilotage.service.mapper.MeetingMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Service for executing complex queries for {@link Meeting} entities in the database.
 * The main input is a {@link MeetingCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link MeetingDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class MeetingQueryService extends QueryService<Meeting> {

    private static final Logger LOG = LoggerFactory.getLogger(MeetingQueryService.class);

    private final MeetingRepository meetingRepository;

    private final MeetingMapper meetingMapper;

    public MeetingQueryService(MeetingRepository meetingRepository, MeetingMapper meetingMapper) {
        this.meetingRepository = meetingRepository;
        this.meetingMapper = meetingMapper;
    }

    /**
     * Return a {@link Page} of {@link MeetingDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<MeetingDTO> findByCriteria(MeetingCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Meeting> specification = createSpecification(criteria);
        return meetingRepository.findAll(specification, page).map(meetingMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(MeetingCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Meeting> specification = createSpecification(criteria);
        return meetingRepository.count(specification);
    }

    /**
     * Function to convert {@link MeetingCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Meeting> createSpecification(MeetingCriteria criteria) {
        Specification<Meeting> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), Meeting_.id),
                buildStringSpecification(criteria.getInstance(), Meeting_.instance),
                buildRangeSpecification(criteria.getScheduledAt(), Meeting_.scheduledAt),
                buildRangeSpecification(criteria.getHeldAt(), Meeting_.heldAt),
                buildSpecification(criteria.getMinutesRef(), Meeting_.minutesRef),
                buildStringSpecification(criteria.getMinutesDigest(), Meeting_.minutesDigest),
                buildSpecification(criteria.getState(), Meeting_.state)
            );
        }
        return specification;
    }
}
