package cm.gimac.des.notification.service;

import cm.gimac.des.notification.domain.MessageTemplate;
import cm.gimac.des.notification.repository.MessageTemplateRepository;
import cm.gimac.des.notification.service.dto.MessageTemplateDTO;
import cm.gimac.des.notification.service.mapper.MessageTemplateMapper;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link cm.gimac.des.notification.domain.MessageTemplate}.
 */
@Service
@Transactional
public class MessageTemplateService {

    private static final Logger LOG = LoggerFactory.getLogger(MessageTemplateService.class);

    private final MessageTemplateRepository messageTemplateRepository;

    private final MessageTemplateMapper messageTemplateMapper;

    public MessageTemplateService(MessageTemplateRepository messageTemplateRepository, MessageTemplateMapper messageTemplateMapper) {
        this.messageTemplateRepository = messageTemplateRepository;
        this.messageTemplateMapper = messageTemplateMapper;
    }

    /**
     * Save a messageTemplate.
     *
     * @param messageTemplateDTO the entity to save.
     * @return the persisted entity.
     */
    public MessageTemplateDTO save(MessageTemplateDTO messageTemplateDTO) {
        LOG.debug("Request to save MessageTemplate : {}", messageTemplateDTO);
        MessageTemplate messageTemplate = messageTemplateMapper.toEntity(messageTemplateDTO);
        messageTemplate = messageTemplateRepository.save(messageTemplate);
        return messageTemplateMapper.toDto(messageTemplate);
    }

    /**
     * Update a messageTemplate.
     *
     * @param messageTemplateDTO the entity to save.
     * @return the persisted entity.
     */
    public MessageTemplateDTO update(MessageTemplateDTO messageTemplateDTO) {
        LOG.debug("Request to update MessageTemplate : {}", messageTemplateDTO);
        MessageTemplate messageTemplate = messageTemplateMapper.toEntity(messageTemplateDTO);
        messageTemplate = messageTemplateRepository.save(messageTemplate);
        return messageTemplateMapper.toDto(messageTemplate);
    }

    /**
     * Partially update a messageTemplate.
     *
     * @param messageTemplateDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<MessageTemplateDTO> partialUpdate(MessageTemplateDTO messageTemplateDTO) {
        LOG.debug("Request to partially update MessageTemplate : {}", messageTemplateDTO);

        return messageTemplateRepository
            .findById(messageTemplateDTO.getId())
            .map(existingMessageTemplate -> {
                messageTemplateMapper.partialUpdate(existingMessageTemplate, messageTemplateDTO);

                return existingMessageTemplate;
            })
            .map(messageTemplateRepository::save)
            .map(messageTemplateMapper::toDto);
    }

    /**
     * Get all the messageTemplates.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<MessageTemplateDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all MessageTemplates");
        return messageTemplateRepository.findAll(pageable).map(messageTemplateMapper::toDto);
    }

    /**
     * Get one messageTemplate by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<MessageTemplateDTO> findOne(UUID id) {
        LOG.debug("Request to get MessageTemplate : {}", id);
        return messageTemplateRepository.findById(id).map(messageTemplateMapper::toDto);
    }

    /**
     * Delete the messageTemplate by id.
     *
     * @param id the id of the entity.
     */
    public void delete(UUID id) {
        LOG.debug("Request to delete MessageTemplate : {}", id);
        messageTemplateRepository.deleteById(id);
    }
}
