package cm.gimac.des.homologation.web.rest;

import cm.gimac.des.homologation.repository.DeadlineNotificationRepository;
import cm.gimac.des.homologation.service.DeadlineNotificationService;
import cm.gimac.des.homologation.service.dto.DeadlineNotificationDTO;
import cm.gimac.des.homologation.web.rest.errors.BadRequestAlertException;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link cm.gimac.des.homologation.domain.DeadlineNotification}.
 */
@RestController
@RequestMapping("/api/deadline-notifications")
public class DeadlineNotificationResource {

    private static final Logger LOG = LoggerFactory.getLogger(DeadlineNotificationResource.class);

    private static final String ENTITY_NAME = "homologationDeadlineNotification";

    @Value("${jhipster.clientApp.name:homologation}")
    private String applicationName;

    private final DeadlineNotificationService deadlineNotificationService;

    private final DeadlineNotificationRepository deadlineNotificationRepository;

    public DeadlineNotificationResource(
        DeadlineNotificationService deadlineNotificationService,
        DeadlineNotificationRepository deadlineNotificationRepository
    ) {
        this.deadlineNotificationService = deadlineNotificationService;
        this.deadlineNotificationRepository = deadlineNotificationRepository;
    }

    /**
     * {@code POST  /deadline-notifications} : Create a new deadlineNotification.
     *
     * @param deadlineNotificationDTO the deadlineNotificationDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new deadlineNotificationDTO, or with status {@code 400 (Bad Request)} if the deadlineNotification already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<DeadlineNotificationDTO> createDeadlineNotification(
        @Valid @RequestBody DeadlineNotificationDTO deadlineNotificationDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to save DeadlineNotification : {}", deadlineNotificationDTO);
        if (deadlineNotificationDTO.getId() != null) {
            throw new BadRequestAlertException("A new deadlineNotification cannot already have an ID", ENTITY_NAME, "idexists");
        }
        deadlineNotificationDTO = deadlineNotificationService.save(deadlineNotificationDTO);
        return ResponseEntity.created(new URI("/api/deadline-notifications/" + deadlineNotificationDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, deadlineNotificationDTO.getId().toString()))
            .body(deadlineNotificationDTO);
    }

    /**
     * {@code PUT  /deadline-notifications/:id} : Updates an existing deadlineNotification.
     *
     * @param id the id of the deadlineNotificationDTO to save.
     * @param deadlineNotificationDTO the deadlineNotificationDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated deadlineNotificationDTO,
     * or with status {@code 400 (Bad Request)} if the deadlineNotificationDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the deadlineNotificationDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<DeadlineNotificationDTO> updateDeadlineNotification(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody DeadlineNotificationDTO deadlineNotificationDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update DeadlineNotification : {}, {}", id, deadlineNotificationDTO);
        if (deadlineNotificationDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, deadlineNotificationDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!deadlineNotificationRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        deadlineNotificationDTO = deadlineNotificationService.update(deadlineNotificationDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, deadlineNotificationDTO.getId().toString()))
            .body(deadlineNotificationDTO);
    }

    /**
     * {@code PATCH  /deadline-notifications/:id} : Partial updates given fields of an existing deadlineNotification, field will ignore if it is null
     *
     * @param id the id of the deadlineNotificationDTO to save.
     * @param deadlineNotificationDTO the deadlineNotificationDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated deadlineNotificationDTO,
     * or with status {@code 400 (Bad Request)} if the deadlineNotificationDTO is not valid,
     * or with status {@code 404 (Not Found)} if the deadlineNotificationDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the deadlineNotificationDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<DeadlineNotificationDTO> partialUpdateDeadlineNotification(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody DeadlineNotificationDTO deadlineNotificationDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partially update DeadlineNotification : {}, {}", id, deadlineNotificationDTO);
        if (deadlineNotificationDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, deadlineNotificationDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!deadlineNotificationRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<DeadlineNotificationDTO> result = deadlineNotificationService.partialUpdate(deadlineNotificationDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, deadlineNotificationDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /deadline-notifications} : get all the Deadline Notifications.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Deadline Notifications in body.
     */
    @GetMapping("")
    public List<DeadlineNotificationDTO> getAllDeadlineNotifications() {
        LOG.debug("REST request to get all DeadlineNotifications");
        return deadlineNotificationService.findAll();
    }

    /**
     * {@code GET  /deadline-notifications/:id} : get the "id" deadlineNotification.
     *
     * @param id the id of the deadlineNotificationDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the deadlineNotificationDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DeadlineNotificationDTO> getDeadlineNotification(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get DeadlineNotification : {}", id);
        Optional<DeadlineNotificationDTO> deadlineNotificationDTO = deadlineNotificationService.findOne(id);
        return ResponseUtil.wrapOrNotFound(deadlineNotificationDTO);
    }

    /**
     * {@code DELETE  /deadline-notifications/:id} : delete the "id" deadlineNotification.
     *
     * @param id the id of the deadlineNotificationDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDeadlineNotification(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete DeadlineNotification : {}", id);
        deadlineNotificationService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
