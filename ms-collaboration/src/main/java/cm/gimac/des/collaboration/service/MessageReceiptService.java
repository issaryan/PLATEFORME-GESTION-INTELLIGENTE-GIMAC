package cm.gimac.des.collaboration.service;

import cm.gimac.des.collaboration.domain.MessageReceipt;
import cm.gimac.des.collaboration.repository.MessageReceiptRepository;
import cm.gimac.des.collaboration.service.dto.MessageReceiptDTO;
import cm.gimac.des.collaboration.service.mapper.MessageReceiptMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.collaboration.domain.MessageReceipt}.
 */
@Service
@Transactional
public class MessageReceiptService {

    private static final Logger LOG = LoggerFactory.getLogger(MessageReceiptService.class);

    private final MessageReceiptRepository messageReceiptRepository;

    private final MessageReceiptMapper messageReceiptMapper;

    public MessageReceiptService(MessageReceiptRepository messageReceiptRepository, MessageReceiptMapper messageReceiptMapper) {
        this.messageReceiptRepository = messageReceiptRepository;
        this.messageReceiptMapper = messageReceiptMapper;
    }

    /**
     * Save a messageReceipt.
     *
     * @param messageReceiptDTO the entity to save.
     * @return the persisted entity.
     */
    public MessageReceiptDTO save(MessageReceiptDTO messageReceiptDTO) {
        LOG.debug("Request to save MessageReceipt : {}", messageReceiptDTO);
        MessageReceipt messageReceipt = messageReceiptMapper.toEntity(messageReceiptDTO);
        messageReceipt = messageReceiptRepository.save(messageReceipt);
        return messageReceiptMapper.toDto(messageReceipt);
    }

    /**
     * Update a messageReceipt.
     *
     * @param messageReceiptDTO the entity to save.
     * @return the persisted entity.
     */
    public MessageReceiptDTO update(MessageReceiptDTO messageReceiptDTO) {
        LOG.debug("Request to update MessageReceipt : {}", messageReceiptDTO);
        MessageReceipt messageReceipt = messageReceiptMapper.toEntity(messageReceiptDTO);
        messageReceipt = messageReceiptRepository.save(messageReceipt);
        return messageReceiptMapper.toDto(messageReceipt);
    }

    /**
     * Partially update a messageReceipt.
     *
     * @param messageReceiptDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<MessageReceiptDTO> partialUpdate(MessageReceiptDTO messageReceiptDTO) {
        LOG.debug("Request to partially update MessageReceipt : {}", messageReceiptDTO);

        return messageReceiptRepository
            .findById(messageReceiptDTO.getId())
            .map(existingMessageReceipt -> {
                messageReceiptMapper.partialUpdate(existingMessageReceipt, messageReceiptDTO);

                return existingMessageReceipt;
            })
            .map(messageReceiptRepository::save)
            .map(messageReceiptMapper::toDto);
    }

    /**
     * Get all the messageReceipts.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<MessageReceiptDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all MessageReceipts");
        return messageReceiptRepository.findAll(pageable).map(messageReceiptMapper::toDto);
    }

    /**
     * Get all the messageReceipts with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<MessageReceiptDTO> findAllWithEagerRelationships(Pageable pageable) {
        return messageReceiptRepository.findAllWithEagerRelationships(pageable).map(messageReceiptMapper::toDto);
    }

    /**
     * Get one messageReceipt by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<MessageReceiptDTO> findOne(UUID id) {
        LOG.debug("Request to get MessageReceipt : {}", id);
        return messageReceiptRepository.findOneWithEagerRelationships(id).map(messageReceiptMapper::toDto);
    }

    /**
     * Delete the messageReceipt by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete MessageReceipt : {}", id);
        messageReceiptRepository.deleteById(id);
    }
}
