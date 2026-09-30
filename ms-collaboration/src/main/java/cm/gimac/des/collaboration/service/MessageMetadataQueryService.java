package cm.gimac.des.collaboration.service;

import cm.gimac.des.collaboration.domain.*; // for static metamodels
import cm.gimac.des.collaboration.domain.MessageMetadata;
import cm.gimac.des.collaboration.repository.MessageMetadataRepository;
import cm.gimac.des.collaboration.service.criteria.MessageMetadataCriteria;
import cm.gimac.des.collaboration.service.dto.MessageMetadataDTO;
import cm.gimac.des.collaboration.service.mapper.MessageMetadataMapper;
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
 * Service for executing complex queries for {@link MessageMetadata} entities in the database.
 * The main input is a {@link MessageMetadataCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link MessageMetadataDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class MessageMetadataQueryService extends QueryService<MessageMetadata> {

    private static final Logger LOG = LoggerFactory.getLogger(MessageMetadataQueryService.class);

    private final MessageMetadataRepository messageMetadataRepository;

    private final MessageMetadataMapper messageMetadataMapper;

    public MessageMetadataQueryService(MessageMetadataRepository messageMetadataRepository, MessageMetadataMapper messageMetadataMapper) {
        this.messageMetadataRepository = messageMetadataRepository;
        this.messageMetadataMapper = messageMetadataMapper;
    }

    /**
     * Return a {@link Page} of {@link MessageMetadataDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<MessageMetadataDTO> findByCriteria(MessageMetadataCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<MessageMetadata> specification = createSpecification(criteria);
        return messageMetadataRepository.findAll(specification, page).map(messageMetadataMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(MessageMetadataCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<MessageMetadata> specification = createSpecification(criteria);
        return messageMetadataRepository.count(specification);
    }

    /**
     * Function to convert {@link MessageMetadataCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<MessageMetadata> createSpecification(MessageMetadataCriteria criteria) {
        Specification<MessageMetadata> specification = Specification.unrestricted();
        specification = specification.and((root, query, builder) -> {
            if (Long.class != query.getResultType()) {
                root.fetch(MessageMetadata_.room, JoinType.LEFT);
            }
            return null;
        });
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = specification.and(
                Specification.allOf(
                    Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : Specification.unrestricted(),
                    buildSpecification(criteria.getId(), MessageMetadata_.id),
                    buildStringSpecification(criteria.getMatrixEventId(), MessageMetadata_.matrixEventId),
                    buildSpecification(criteria.getAuthorId(), MessageMetadata_.authorId),
                    buildStringSpecification(criteria.getTerminalId(), MessageMetadata_.terminalId),
                    buildRangeSpecification(criteria.getSentAt(), MessageMetadata_.sentAt),
                    buildStringSpecification(criteria.getSessionId(), MessageMetadata_.sessionId),
                    buildRangeSpecification(criteria.getRatchetIndex(), MessageMetadata_.ratchetIndex),
                    buildRangeSpecification(criteria.getCiphertextSize(), MessageMetadata_.ciphertextSize),
                    buildStringSpecification(criteria.getAttachmentRefs(), MessageMetadata_.attachmentRefs),
                    buildSpecification(criteria.getRoomId(), root -> root.join(MessageMetadata_.room, JoinType.LEFT).get(Room_.id))
                )
            );
        }
        return specification;
    }
}
