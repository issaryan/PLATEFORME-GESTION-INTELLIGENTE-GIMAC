package cm.gimac.des.notification.web.rest;

import cm.gimac.des.notification.repository.NotificationSubscriptionRepository;
import cm.gimac.des.notification.service.NotificationSubscriptionQueryService;
import cm.gimac.des.notification.service.NotificationSubscriptionService;
import cm.gimac.des.notification.service.criteria.NotificationSubscriptionCriteria;
import cm.gimac.des.notification.service.dto.NotificationSubscriptionDTO;
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
 * REST controller for managing {@link cm.gimac.des.notification.domain.NotificationSubscription}.
 */
@RestController
@RequestMapping("/api/notification-subscriptions")
public class NotificationSubscriptionResource {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationSubscriptionResource.class);

    private static final String ENTITY_NAME = "notificationNotificationSubscription";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final NotificationSubscriptionService notificationSubscriptionService;

    private final NotificationSubscriptionRepository notificationSubscriptionRepository;

    private final NotificationSubscriptionQueryService notificationSubscriptionQueryService;

    public NotificationSubscriptionResource(
        NotificationSubscriptionService notificationSubscriptionService,
        NotificationSubscriptionRepository notificationSubscriptionRepository,
        NotificationSubscriptionQueryService notificationSubscriptionQueryService
    ) {
        this.notificationSubscriptionService = notificationSubscriptionService;
        this.notificationSubscriptionRepository = notificationSubscriptionRepository;
        this.notificationSubscriptionQueryService = notificationSubscriptionQueryService;
    }

    /**
     * {@code POST  /notification-subscriptions} : Create a new notificationSubscription.
     *
     * @param notificationSubscriptionDTO the notificationSubscriptionDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new notificationSubscriptionDTO, or with status {@code 400 (Bad Request)} if the notificationSubscription has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<NotificationSubscriptionDTO> createNotificationSubscription(
        @Valid @RequestBody NotificationSubscriptionDTO notificationSubscriptionDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to save NotificationSubscription : {}", notificationSubscriptionDTO);
        if (notificationSubscriptionDTO.getId() != null) {
            throw new BadRequestAlertException("A new notificationSubscription cannot already have an ID", ENTITY_NAME, "idexists");
        }
        notificationSubscriptionDTO = notificationSubscriptionService.save(notificationSubscriptionDTO);
        return ResponseEntity.created(new URI("/api/notification-subscriptions/" + notificationSubscriptionDTO.getId()))
            .headers(
                HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, notificationSubscriptionDTO.getId().toString())
            )
            .body(notificationSubscriptionDTO);
    }

    /**
     * {@code PUT  /notification-subscriptions/:id} : Updates an existing notificationSubscription.
     *
     * @param id the id of the notificationSubscriptionDTO to save.
     * @param notificationSubscriptionDTO the notificationSubscriptionDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated notificationSubscriptionDTO,
     * or with status {@code 400 (Bad Request)} if the notificationSubscriptionDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the notificationSubscriptionDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<NotificationSubscriptionDTO> updateNotificationSubscription(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody NotificationSubscriptionDTO notificationSubscriptionDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update NotificationSubscription : {}, {}", id, notificationSubscriptionDTO);
        if (notificationSubscriptionDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, notificationSubscriptionDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!notificationSubscriptionRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        notificationSubscriptionDTO = notificationSubscriptionService.update(notificationSubscriptionDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, notificationSubscriptionDTO.getId().toString()))
            .body(notificationSubscriptionDTO);
    }

    /**
     * {@code PATCH  /notification-subscriptions/:id} : Partial updates given fields of an existing notificationSubscription, field will ignore if it is null
     *
     * @param id the id of the notificationSubscriptionDTO to save.
     * @param notificationSubscriptionDTO the notificationSubscriptionDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated notificationSubscriptionDTO,
     * or with status {@code 400 (Bad Request)} if the notificationSubscriptionDTO is not valid,
     * or with status {@code 404 (Not Found)} if the notificationSubscriptionDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the notificationSubscriptionDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<NotificationSubscriptionDTO> partialUpdateNotificationSubscription(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody NotificationSubscriptionDTO notificationSubscriptionDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update NotificationSubscription partially : {}, {}", id, notificationSubscriptionDTO);
        if (notificationSubscriptionDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, notificationSubscriptionDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!notificationSubscriptionRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<NotificationSubscriptionDTO> result = notificationSubscriptionService.partialUpdate(notificationSubscriptionDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, notificationSubscriptionDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /notification-subscriptions} : get all the notificationSubscriptions.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of notificationSubscriptions in body.
     */
    @GetMapping("")
    public ResponseEntity<List<NotificationSubscriptionDTO>> getAllNotificationSubscriptions(
        NotificationSubscriptionCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get NotificationSubscriptions by criteria: {}", criteria);

        Page<NotificationSubscriptionDTO> page = notificationSubscriptionQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /notification-subscriptions/count} : count all the notificationSubscriptions.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countNotificationSubscriptions(NotificationSubscriptionCriteria criteria) {
        LOG.debug("REST request to count NotificationSubscriptions by criteria: {}", criteria);
        return ResponseEntity.ok().body(notificationSubscriptionQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /notification-subscriptions/:id} : get the "id" notificationSubscription.
     *
     * @param id the id of the notificationSubscriptionDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the notificationSubscriptionDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<NotificationSubscriptionDTO> getNotificationSubscription(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get NotificationSubscription : {}", id);
        Optional<NotificationSubscriptionDTO> notificationSubscriptionDTO = notificationSubscriptionService.findOne(id);
        return ResponseUtil.wrapOrNotFound(notificationSubscriptionDTO);
    }

    /**
     * {@code DELETE  /notification-subscriptions/:id} : delete the "id" notificationSubscription.
     *
     * @param id the id of the notificationSubscriptionDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotificationSubscription(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete NotificationSubscription : {}", id);
        notificationSubscriptionService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
