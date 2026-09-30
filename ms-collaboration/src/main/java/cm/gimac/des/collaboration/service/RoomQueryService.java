package cm.gimac.des.collaboration.service;

import cm.gimac.des.collaboration.domain.*; // for static metamodels
import cm.gimac.des.collaboration.domain.Room;
import cm.gimac.des.collaboration.repository.RoomRepository;
import cm.gimac.des.collaboration.service.criteria.RoomCriteria;
import cm.gimac.des.collaboration.service.dto.RoomDTO;
import cm.gimac.des.collaboration.service.mapper.RoomMapper;
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
 * Service for executing complex queries for {@link Room} entities in the database.
 * The main input is a {@link RoomCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link RoomDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class RoomQueryService extends QueryService<Room> {

    private static final Logger LOG = LoggerFactory.getLogger(RoomQueryService.class);

    private final RoomRepository roomRepository;

    private final RoomMapper roomMapper;

    public RoomQueryService(RoomRepository roomRepository, RoomMapper roomMapper) {
        this.roomRepository = roomRepository;
        this.roomMapper = roomMapper;
    }

    /**
     * Return a {@link Page} of {@link RoomDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<RoomDTO> findByCriteria(RoomCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Room> specification = createSpecification(criteria);
        return roomRepository.findAll(specification, page).map(roomMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(RoomCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Room> specification = createSpecification(criteria);
        return roomRepository.count(specification);
    }

    /**
     * Function to convert {@link RoomCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Room> createSpecification(RoomCriteria criteria) {
        Specification<Room> specification = Specification.unrestricted();
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = specification.and(
                Specification.allOf(
                    Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : Specification.unrestricted(),
                    buildSpecification(criteria.getId(), Room_.id),
                    buildStringSpecification(criteria.getCode(), Room_.code),
                    buildSpecification(criteria.getType(), Room_.type),
                    buildSpecification(criteria.getAttachmentKind(), Room_.attachmentKind),
                    buildStringSpecification(criteria.getAttachmentReference(), Room_.attachmentReference),
                    buildStringSpecification(criteria.getAttachmentService(), Room_.attachmentService),
                    buildStringSpecification(criteria.getTicketReference(), Room_.ticketReference),
                    buildStringSpecification(criteria.getMatrixRoomId(), Room_.matrixRoomId),
                    buildStringSpecification(criteria.getEncryption(), Room_.encryption),
                    buildRangeSpecification(criteria.getCreatedAt(), Room_.createdAt),
                    buildSpecification(criteria.getState(), Room_.state),
                    buildRangeSpecification(criteria.getRetention(), Room_.retention),
                    buildRangeSpecification(criteria.getGroupSessionEpoch(), Room_.groupSessionEpoch),
                    buildRangeSpecification(criteria.getFrozenAt(), Room_.frozenAt),
                    buildRangeSpecification(criteria.getArchivedAt(), Room_.archivedAt),
                    buildRangeSpecification(criteria.getPurgeAfter(), Room_.purgeAfter),
                    buildSpecification(criteria.getRoomArchiveId(), root ->
                        root.join(Room_.roomArchive, JoinType.LEFT).get(RoomArchive_.id)
                    )
                )
            );
        }
        return specification;
    }
}
