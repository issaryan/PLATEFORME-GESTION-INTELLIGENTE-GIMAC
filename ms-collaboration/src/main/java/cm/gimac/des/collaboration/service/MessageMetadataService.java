package cm.gimac.des.collaboration.service;

import cm.gimac.des.collaboration.domain.MessageMetadata;
import cm.gimac.des.collaboration.repository.MessageMetadataRepository;
import cm.gimac.des.collaboration.service.dto.MessageMetadataDTO;
import cm.gimac.des.collaboration.service.mapper.MessageMetadataMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.collaboration.domain.MessageMetadata}.
 */
@Service
@Transactional
public class MessageMetadataService {

    private static final Logger LOG = LoggerFactory.getLogger(MessageMetadataService.class);

    private final MessageMetadataRepository messageMetadataRepository;

    private final MessageMetadataMapper messageMetadataMapper;

    public MessageMetadataService(MessageMetadataRepository messageMetadataRepository, MessageMetadataMapper messageMetadataMapper) {
        this.messageMetadataRepository = messageMetadataRepository;
        this.messageMetadataMapper = messageMetadataMapper;
    }

    /**
     * Save a messageMetadata.
     *
     * @param messageMetadataDTO the entity to save.
     * @return the persisted entity.
     */
    public MessageMetadataDTO save(MessageMetadataDTO messageMetadataDTO) {
        LOG.debug("Request to save MessageMetadata : {}", messageMetadataDTO);
        MessageMetadata messageMetadata = messageMetadataMapper.toEntity(messageMetadataDTO);
        messageMetadata = messageMetadataRepository.save(messageMetadata);
        return messageMetadataMapper.toDto(messageMetadata);
    }

    /**
     * Update a messageMetadata.
     *
     * @param messageMetadataDTO the entity to save.
     * @return the persisted entity.
     */
    public MessageMetadataDTO update(MessageMetadataDTO messageMetadataDTO) {
        LOG.debug("Request to update MessageMetadata : {}", messageMetadataDTO);
        MessageMetadata messageMetadata = messageMetadataMapper.toEntity(messageMetadataDTO);
        messageMetadata = messageMetadataRepository.save(messageMetadata);
        return messageMetadataMapper.toDto(messageMetadata);
    }

    /**
     * Partially update a messageMetadata.
     *
     * @param messageMetadataDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<MessageMetadataDTO> partialUpdate(MessageMetadataDTO messageMetadataDTO) {
        LOG.debug("Request to partially update MessageMetadata : {}", messageMetadataDTO);

        return messageMetadataRepository
            .findById(messageMetadataDTO.getId())
            .map(existingMessageMetadata -> {
                messageMetadataMapper.partialUpdate(existingMessageMetadata, messageMetadataDTO);

                return existingMessageMetadata;
            })
            .map(messageMetadataRepository::save)
            .map(messageMetadataMapper::toDto);
    }

    /**
     * Get all the messageMetadatas with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<MessageMetadataDTO> findAllWithEagerRelationships(Pageable pageable) {
        return messageMetadataRepository.findAllWithEagerRelationships(pageable).map(messageMetadataMapper::toDto);
    }

    /**
     * Get one messageMetadata by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<MessageMetadataDTO> findOne(UUID id) {
        LOG.debug("Request to get MessageMetadata : {}", id);
        return messageMetadataRepository.findOneWithEagerRelationships(id).map(messageMetadataMapper::toDto);
    }

    /**
     * Delete the messageMetadata by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete MessageMetadata : {}", id);
        messageMetadataRepository.deleteById(id);
    }
}
