package cm.gimac.des.collaboration.service;

import cm.gimac.des.collaboration.domain.*; // for static metamodels
import cm.gimac.des.collaboration.domain.RoomArchive;
import cm.gimac.des.collaboration.repository.RoomArchiveRepository;
import cm.gimac.des.collaboration.service.criteria.RoomArchiveCriteria;
import cm.gimac.des.collaboration.service.dto.RoomArchiveDTO;
import cm.gimac.des.collaboration.service.mapper.RoomArchiveMapper;
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
 * Service for executing complex queries for {@link RoomArchive} entities in the database.
 * The main input is a {@link RoomArchiveCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link RoomArchiveDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class RoomArchiveQueryService extends QueryService<RoomArchive> {

    private static final Logger LOG = LoggerFactory.getLogger(RoomArchiveQueryService.class);

    private final RoomArchiveRepository roomArchiveRepository;

    private final RoomArchiveMapper roomArchiveMapper;

    public RoomArchiveQueryService(RoomArchiveRepository roomArchiveRepository, RoomArchiveMapper roomArchiveMapper) {
        this.roomArchiveRepository = roomArchiveRepository;
        this.roomArchiveMapper = roomArchiveMapper;
    }

    /**
     * Return a {@link Page} of {@link RoomArchiveDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<RoomArchiveDTO> findByCriteria(RoomArchiveCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<RoomArchive> specification = createSpecification(criteria);
        return roomArchiveRepository.findAll(specification, page).map(roomArchiveMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(RoomArchiveCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<RoomArchive> specification = createSpecification(criteria);
        return roomArchiveRepository.count(specification);
    }

    /**
     * Function to convert {@link RoomArchiveCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<RoomArchive> createSpecification(RoomArchiveCriteria criteria) {
        Specification<RoomArchive> specification = Specification.unrestricted();
        specification = specification.and((root, query, builder) -> {
            if (Long.class != query.getResultType()) {
                root.fetch(RoomArchive_.room, JoinType.LEFT);
            }
            return null;
        });
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = specification.and(
                Specification.allOf(
                    Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : Specification.unrestricted(),
                    buildSpecification(criteria.getId(), RoomArchive_.id),
                    buildStringSpecification(criteria.getCrisisCellReference(), RoomArchive_.crisisCellReference),
                    buildStringSpecification(criteria.getTicketReference(), RoomArchive_.ticketReference),
                    buildRangeSpecification(criteria.getMessageCount(), RoomArchive_.messageCount),
                    buildRangeSpecification(criteria.getUndecryptableCount(), RoomArchive_.undecryptableCount),
                    buildRangeSpecification(criteria.getMaskedSequences(), RoomArchive_.maskedSequences),
                    buildStringSpecification(criteria.getChainHead(), RoomArchive_.chainHead),
                    buildStringSpecification(criteria.getContentDigest(), RoomArchive_.contentDigest),
                    buildSpecification(criteria.getDocumentRef(), RoomArchive_.documentRef),
                    buildRangeSpecification(criteria.getRetentionYears(), RoomArchive_.retentionYears),
                    buildSpecification(criteria.getState(), RoomArchive_.state),
                    buildRangeSpecification(criteria.getStartedAt(), RoomArchive_.startedAt),
                    buildRangeSpecification(criteria.getSealedAt(), RoomArchive_.sealedAt),
                    buildSpecification(criteria.getRoomId(), root -> root.join(RoomArchive_.room, JoinType.LEFT).get(Room_.id))
                )
            );
        }
        return specification;
    }
}
