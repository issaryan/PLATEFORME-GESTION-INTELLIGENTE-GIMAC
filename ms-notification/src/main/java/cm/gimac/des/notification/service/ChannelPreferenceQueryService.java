package cm.gimac.des.notification.service;

import cm.gimac.des.notification.domain.*; // for static metamodels
import cm.gimac.des.notification.domain.ChannelPreference;
import cm.gimac.des.notification.repository.ChannelPreferenceRepository;
import cm.gimac.des.notification.service.criteria.ChannelPreferenceCriteria;
import cm.gimac.des.notification.service.dto.ChannelPreferenceDTO;
import cm.gimac.des.notification.service.mapper.ChannelPreferenceMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Service for executing complex queries for {@link ChannelPreference} entities in the database.
 * The main input is a {@link ChannelPreferenceCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link ChannelPreferenceDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class ChannelPreferenceQueryService extends QueryService<ChannelPreference> {

    private static final Logger LOG = LoggerFactory.getLogger(ChannelPreferenceQueryService.class);

    private final ChannelPreferenceRepository channelPreferenceRepository;

    private final ChannelPreferenceMapper channelPreferenceMapper;

    public ChannelPreferenceQueryService(
        ChannelPreferenceRepository channelPreferenceRepository,
        ChannelPreferenceMapper channelPreferenceMapper
    ) {
        this.channelPreferenceRepository = channelPreferenceRepository;
        this.channelPreferenceMapper = channelPreferenceMapper;
    }

    /**
     * Return a {@link Page} of {@link ChannelPreferenceDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<ChannelPreferenceDTO> findByCriteria(ChannelPreferenceCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<ChannelPreference> specification = createSpecification(criteria);
        return channelPreferenceRepository.findAll(specification, page).map(channelPreferenceMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(ChannelPreferenceCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<ChannelPreference> specification = createSpecification(criteria);
        return channelPreferenceRepository.count(specification);
    }

    /**
     * Function to convert {@link ChannelPreferenceCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<ChannelPreference> createSpecification(ChannelPreferenceCriteria criteria) {
        Specification<ChannelPreference> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), ChannelPreference_.id),
                buildSpecification(criteria.getUserId(), ChannelPreference_.userId),
                buildSpecification(criteria.getChannel(), ChannelPreference_.channel),
                buildSpecification(criteria.getMinimumUrgency(), ChannelPreference_.minimumUrgency),
                buildRangeSpecification(criteria.getWindowStart(), ChannelPreference_.windowStart),
                buildRangeSpecification(criteria.getWindowEnd(), ChannelPreference_.windowEnd),
                buildSpecification(criteria.getEnabled(), ChannelPreference_.enabled)
            );
        }
        return specification;
    }
}
