package cm.gimac.des.notification.web.rest;

import cm.gimac.des.notification.repository.MessageTemplateRepository;
import cm.gimac.des.notification.service.MessageTemplateService;
import cm.gimac.des.notification.service.dto.MessageTemplateDTO;
import cm.gimac.des.notification.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link cm.gimac.des.notification.domain.MessageTemplate}.
 */
@RestController
@RequestMapping("/api/message-templates")
public class MessageTemplateResource {

    private static final Logger LOG = LoggerFactory.getLogger(MessageTemplateResource.class);

    private static final String ENTITY_NAME = "notificationMessageTemplate";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final MessageTemplateService messageTemplateService;

    private final MessageTemplateRepository messageTemplateRepository;

    public MessageTemplateResource(MessageTemplateService messageTemplateService, MessageTemplateRepository messageTemplateRepository) {
        this.messageTemplateService = messageTemplateService;
        this.messageTemplateRepository = messageTemplateRepository;
    }

    /**
     * {@code POST  /message-templates} : Create a new messageTemplate.
     *
     * @param messageTemplateDTO the messageTemplateDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new messageTemplateDTO, or with status {@code 400 (Bad Request)} if the messageTemplate has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<MessageTemplateDTO> createMessageTemplate(@Valid @RequestBody MessageTemplateDTO messageTemplateDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save MessageTemplate : {}", messageTemplateDTO);
        if (messageTemplateDTO.getId() != null) {
            throw new BadRequestAlertException("A new messageTemplate cannot already have an ID", ENTITY_NAME, "idexists");
        }
        messageTemplateDTO = messageTemplateService.save(messageTemplateDTO);
        return ResponseEntity.created(new URI("/api/message-templates/" + messageTemplateDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, messageTemplateDTO.getId().toString()))
            .body(messageTemplateDTO);
    }

    /**
     * {@code PUT  /message-templates/:id} : Updates an existing messageTemplate.
     *
     * @param id the id of the messageTemplateDTO to save.
     * @param messageTemplateDTO the messageTemplateDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated messageTemplateDTO,
     * or with status {@code 400 (Bad Request)} if the messageTemplateDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the messageTemplateDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<MessageTemplateDTO> updateMessageTemplate(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody MessageTemplateDTO messageTemplateDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update MessageTemplate : {}, {}", id, messageTemplateDTO);
        if (messageTemplateDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, messageTemplateDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!messageTemplateRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        messageTemplateDTO = messageTemplateService.update(messageTemplateDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, messageTemplateDTO.getId().toString()))
            .body(messageTemplateDTO);
    }

    /**
     * {@code PATCH  /message-templates/:id} : Partial updates given fields of an existing messageTemplate, field will ignore if it is null
     *
     * @param id the id of the messageTemplateDTO to save.
     * @param messageTemplateDTO the messageTemplateDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated messageTemplateDTO,
     * or with status {@code 400 (Bad Request)} if the messageTemplateDTO is not valid,
     * or with status {@code 404 (Not Found)} if the messageTemplateDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the messageTemplateDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<MessageTemplateDTO> partialUpdateMessageTemplate(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody MessageTemplateDTO messageTemplateDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update MessageTemplate partially : {}, {}", id, messageTemplateDTO);
        if (messageTemplateDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, messageTemplateDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!messageTemplateRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<MessageTemplateDTO> result = messageTemplateService.partialUpdate(messageTemplateDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, messageTemplateDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /message-templates} : get all the messageTemplates.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of messageTemplates in body.
     */
    @GetMapping("")
    public ResponseEntity<List<MessageTemplateDTO>> getAllMessageTemplates(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get a page of MessageTemplates");
        Page<MessageTemplateDTO> page = messageTemplateService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /message-templates/:id} : get the "id" messageTemplate.
     *
     * @param id the id of the messageTemplateDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the messageTemplateDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<MessageTemplateDTO> getMessageTemplate(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get MessageTemplate : {}", id);
        Optional<MessageTemplateDTO> messageTemplateDTO = messageTemplateService.findOne(id);
        return ResponseUtil.wrapOrNotFound(messageTemplateDTO);
    }

    /**
     * {@code DELETE  /message-templates/:id} : delete the "id" messageTemplate.
     *
     * @param id the id of the messageTemplateDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMessageTemplate(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete MessageTemplate : {}", id);
        messageTemplateService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
