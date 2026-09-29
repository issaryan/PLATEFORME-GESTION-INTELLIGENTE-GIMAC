package cm.gimac.des.participant.web.rest;

import cm.gimac.des.participant.repository.SubscribedServiceRepository;
import cm.gimac.des.participant.service.SubscribedServiceQueryService;
import cm.gimac.des.participant.service.SubscribedServiceService;
import cm.gimac.des.participant.service.criteria.SubscribedServiceCriteria;
import cm.gimac.des.participant.service.dto.SubscribedServiceDTO;
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
 * REST controller for managing {@link cm.gimac.des.participant.domain.SubscribedService}.
 */
@RestController
@RequestMapping("/api/subscribed-services")
public class SubscribedServiceResource {

    private static final Logger LOG = LoggerFactory.getLogger(SubscribedServiceResource.class);

    private static final String ENTITY_NAME = "participantSubscribedService";

    @Value("${jhipster.clientApp.name:participant}")
    private String applicationName;

    private final SubscribedServiceService subscribedServiceService;

    private final SubscribedServiceRepository subscribedServiceRepository;

    private final SubscribedServiceQueryService subscribedServiceQueryService;

    public SubscribedServiceResource(
        SubscribedServiceService subscribedServiceService,
        SubscribedServiceRepository subscribedServiceRepository,
        SubscribedServiceQueryService subscribedServiceQueryService
    ) {
        this.subscribedServiceService = subscribedServiceService;
        this.subscribedServiceRepository = subscribedServiceRepository;
        this.subscribedServiceQueryService = subscribedServiceQueryService;
    }

    /**
     * {@code POST  /subscribed-services} : Create a new subscribedService.
     *
     * @param subscribedServiceDTO the subscribedServiceDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new subscribedServiceDTO, or with status {@code 400 (Bad Request)} if the subscribedService already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<SubscribedServiceDTO> createSubscribedService(@Valid @RequestBody SubscribedServiceDTO subscribedServiceDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save SubscribedService : {}", subscribedServiceDTO);
        if (subscribedServiceDTO.getId() != null) {
            throw new BadRequestAlertException("A new subscribedService cannot already have an ID", ENTITY_NAME, "idexists");
        }
        subscribedServiceDTO = subscribedServiceService.save(subscribedServiceDTO);
        return ResponseEntity.created(new URI("/api/subscribed-services/" + subscribedServiceDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, subscribedServiceDTO.getId().toString()))
            .body(subscribedServiceDTO);
    }

    /**
     * {@code PUT  /subscribed-services/:id} : Updates an existing subscribedService.
     *
     * @param id the id of the subscribedServiceDTO to save.
     * @param subscribedServiceDTO the subscribedServiceDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated subscribedServiceDTO,
     * or with status {@code 400 (Bad Request)} if the subscribedServiceDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the subscribedServiceDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<SubscribedServiceDTO> updateSubscribedService(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody SubscribedServiceDTO subscribedServiceDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update SubscribedService : {}, {}", id, subscribedServiceDTO);
        if (subscribedServiceDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, subscribedServiceDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!subscribedServiceRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        subscribedServiceDTO = subscribedServiceService.update(subscribedServiceDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, subscribedServiceDTO.getId().toString()))
            .body(subscribedServiceDTO);
    }

    /**
     * {@code PATCH  /subscribed-services/:id} : Partial updates given fields of an existing subscribedService, field will ignore if it is null
     *
     * @param id the id of the subscribedServiceDTO to save.
     * @param subscribedServiceDTO the subscribedServiceDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated subscribedServiceDTO,
     * or with status {@code 400 (Bad Request)} if the subscribedServiceDTO is not valid,
     * or with status {@code 404 (Not Found)} if the subscribedServiceDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the subscribedServiceDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<SubscribedServiceDTO> partialUpdateSubscribedService(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody SubscribedServiceDTO subscribedServiceDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partially update SubscribedService : {}, {}", id, subscribedServiceDTO);
        if (subscribedServiceDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, subscribedServiceDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!subscribedServiceRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<SubscribedServiceDTO> result = subscribedServiceService.partialUpdate(subscribedServiceDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, subscribedServiceDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /subscribed-services} : get all the Subscribed Services.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Subscribed Services in body.
     */
    @GetMapping("")
    public ResponseEntity<List<SubscribedServiceDTO>> getAllSubscribedServices(
        SubscribedServiceCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get SubscribedServices by criteria: {}", criteria);

        Page<SubscribedServiceDTO> page = subscribedServiceQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /subscribed-services/count} : count all the subscribedServices.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countSubscribedServices(SubscribedServiceCriteria criteria) {
        LOG.debug("REST request to count SubscribedServices by criteria: {}", criteria);
        return ResponseEntity.ok().body(subscribedServiceQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /subscribed-services/:id} : get the "id" subscribedService.
     *
     * @param id the id of the subscribedServiceDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the subscribedServiceDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SubscribedServiceDTO> getSubscribedService(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get SubscribedService : {}", id);
        Optional<SubscribedServiceDTO> subscribedServiceDTO = subscribedServiceService.findOne(id);
        return ResponseUtil.wrapOrNotFound(subscribedServiceDTO);
    }

    /**
     * {@code DELETE  /subscribed-services/:id} : delete the "id" subscribedService.
     *
     * @param id the id of the subscribedServiceDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubscribedService(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete SubscribedService : {}", id);
        subscribedServiceService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
