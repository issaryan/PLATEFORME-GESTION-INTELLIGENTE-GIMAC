package cm.gimac.des.participant.web.rest;

import cm.gimac.des.participant.repository.EscalationContactRepository;
import cm.gimac.des.participant.service.EscalationContactQueryService;
import cm.gimac.des.participant.service.EscalationContactService;
import cm.gimac.des.participant.service.criteria.EscalationContactCriteria;
import cm.gimac.des.participant.service.dto.EscalationContactDTO;
import cm.gimac.des.participant.web.rest.errors.BadRequestAlertException;
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
 * REST controller for managing {@link cm.gimac.des.participant.domain.EscalationContact}.
 */
@RestController
@RequestMapping("/api/escalation-contacts")
public class EscalationContactResource {

    private static final Logger LOG = LoggerFactory.getLogger(EscalationContactResource.class);

    private static final String ENTITY_NAME = "participantEscalationContact";

    @Value("${jhipster.clientApp.name:participant}")
    private String applicationName;

    private final EscalationContactService escalationContactService;

    private final EscalationContactRepository escalationContactRepository;

    private final EscalationContactQueryService escalationContactQueryService;

    public EscalationContactResource(
        EscalationContactService escalationContactService,
        EscalationContactRepository escalationContactRepository,
        EscalationContactQueryService escalationContactQueryService
    ) {
        this.escalationContactService = escalationContactService;
        this.escalationContactRepository = escalationContactRepository;
        this.escalationContactQueryService = escalationContactQueryService;
    }

    /**
     * {@code POST  /escalation-contacts} : Create a new escalationContact.
     *
     * @param escalationContactDTO the escalationContactDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new escalationContactDTO, or with status {@code 400 (Bad Request)} if the escalationContact already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<EscalationContactDTO> createEscalationContact(@Valid @RequestBody EscalationContactDTO escalationContactDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save EscalationContact : {}", escalationContactDTO);
        if (escalationContactDTO.getId() != null) {
            throw new BadRequestAlertException("A new escalationContact cannot already have an ID", ENTITY_NAME, "idexists");
        }
        escalationContactDTO = escalationContactService.save(escalationContactDTO);
        return ResponseEntity.created(new URI("/api/escalation-contacts/" + escalationContactDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, escalationContactDTO.getId().toString()))
            .body(escalationContactDTO);
    }

    /**
     * {@code PUT  /escalation-contacts/:id} : Updates an existing escalationContact.
     *
     * @param id the id of the escalationContactDTO to save.
     * @param escalationContactDTO the escalationContactDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated escalationContactDTO,
     * or with status {@code 400 (Bad Request)} if the escalationContactDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the escalationContactDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<EscalationContactDTO> updateEscalationContact(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody EscalationContactDTO escalationContactDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update EscalationContact : {}, {}", id, escalationContactDTO);
        if (escalationContactDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, escalationContactDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!escalationContactRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        escalationContactDTO = escalationContactService.update(escalationContactDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, escalationContactDTO.getId().toString()))
            .body(escalationContactDTO);
    }

    /**
     * {@code PATCH  /escalation-contacts/:id} : Partial updates given fields of an existing escalationContact, field will ignore if it is null
     *
     * @param id the id of the escalationContactDTO to save.
     * @param escalationContactDTO the escalationContactDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated escalationContactDTO,
     * or with status {@code 400 (Bad Request)} if the escalationContactDTO is not valid,
     * or with status {@code 404 (Not Found)} if the escalationContactDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the escalationContactDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<EscalationContactDTO> partialUpdateEscalationContact(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody EscalationContactDTO escalationContactDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partially update EscalationContact : {}, {}", id, escalationContactDTO);
        if (escalationContactDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, escalationContactDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!escalationContactRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<EscalationContactDTO> result = escalationContactService.partialUpdate(escalationContactDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, escalationContactDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /escalation-contacts} : get all the Escalation Contacts.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Escalation Contacts in body.
     */
    @GetMapping("")
    public ResponseEntity<List<EscalationContactDTO>> getAllEscalationContacts(
        EscalationContactCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get EscalationContacts by criteria: {}", criteria);

        Page<EscalationContactDTO> page = escalationContactQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /escalation-contacts/count} : count all the escalationContacts.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countEscalationContacts(EscalationContactCriteria criteria) {
        LOG.debug("REST request to count EscalationContacts by criteria: {}", criteria);
        return ResponseEntity.ok().body(escalationContactQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /escalation-contacts/:id} : get the "id" escalationContact.
     *
     * @param id the id of the escalationContactDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the escalationContactDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<EscalationContactDTO> getEscalationContact(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get EscalationContact : {}", id);
        Optional<EscalationContactDTO> escalationContactDTO = escalationContactService.findOne(id);
        return ResponseUtil.wrapOrNotFound(escalationContactDTO);
    }

    /**
     * {@code DELETE  /escalation-contacts/:id} : delete the "id" escalationContact.
     *
     * @param id the id of the escalationContactDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEscalationContact(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete EscalationContact : {}", id);
        escalationContactService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
