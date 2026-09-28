package cm.gimac.des.collaboration.service;

import cm.gimac.des.collaboration.domain.*; // for static metamodels
import cm.gimac.des.collaboration.domain.RoomMember;
import cm.gimac.des.collaboration.repository.RoomMemberRepository;
import cm.gimac.des.collaboration.service.criteria.RoomMemberCriteria;
import cm.gimac.des.collaboration.service.dto.RoomMemberDTO;
import cm.gimac.des.collaboration.service.mapper.RoomMemberMapper;
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
 * Service for executing complex queries for {@link RoomMember} entities in the database.
 * The main input is a {@link RoomMemberCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link RoomMemberDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class RoomMemberQueryService extends QueryService<RoomMember> {

    private static final Logger LOG = LoggerFactory.getLogger(RoomMemberQueryService.class);

    private final RoomMemberRepository roomMemberRepository;

    private final RoomMemberMapper roomMemberMapper;

    public RoomMemberQueryService(RoomMemberRepository roomMemberRepository, RoomMemberMapper roomMemberMapper) {
        this.roomMemberRepository = roomMemberRepository;
        this.roomMemberMapper = roomMemberMapper;
    }

    /**
     * Return a {@link Page} of {@link RoomMemberDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<RoomMemberDTO> findByCriteria(RoomMemberCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<RoomMember> specification = createSpecification(criteria);
        return roomMemberRepository.findAll(specification, page).map(roomMemberMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(RoomMemberCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<RoomMember> specification = createSpecification(criteria);
        return roomMemberRepository.count(specification);
    }

    /**
     * Function to convert {@link RoomMemberCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<RoomMember> createSpecification(RoomMemberCriteria criteria) {
        Specification<RoomMember> specification = Specification.where(null);
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = Specification.allOf(
                Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : null,
                buildSpecification(criteria.getId(), RoomMember_.id),
                buildSpecification(criteria.getUserId(), RoomMember_.userId),
                buildSpecification(criteria.getRole(), RoomMember_.role),
                buildRangeSpecification(criteria.getInvitedAt(), RoomMember_.invitedAt),
                buildRangeSpecification(criteria.getAcknowledgedAt(), RoomMember_.acknowledgedAt),
                buildSpecification(criteria.getReplacesUserId(), RoomMember_.replacesUserId),
                buildRangeSpecification(criteria.getJoinedAt(), RoomMember_.joinedAt),
                buildRangeSpecification(criteria.getLeftAt(), RoomMember_.leftAt),
                buildSpecification(criteria.getRoomId(), root -> root.join(RoomMember_.room, JoinType.LEFT).get(Room_.id))
            );
        }
        return specification;
    }
}
