package cm.gimac.des.document.web.rest;

import cm.gimac.des.document.repository.PurgeRequestRepository;
import cm.gimac.des.document.service.PurgeRequestQueryService;
import cm.gimac.des.document.service.PurgeRequestService;
import cm.gimac.des.document.service.criteria.PurgeRequestCriteria;
import cm.gimac.des.document.service.dto.PurgeRequestDTO;
import cm.gimac.des.document.web.rest.errors.BadRequestAlertException;
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
 * REST controller for managing {@link cm.gimac.des.document.domain.PurgeRequest}.
 */
@RestController
@RequestMapping("/api/purge-requests")
public class PurgeRequestResource {

    private static final Logger LOG = LoggerFactory.getLogger(PurgeRequestResource.class);

    private static final String ENTITY_NAME = "documentPurgeRequest";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final PurgeRequestService purgeRequestService;

    private final PurgeRequestRepository purgeRequestRepository;

    private final PurgeRequestQueryService purgeRequestQueryService;

    public PurgeRequestResource(
        PurgeRequestService purgeRequestService,
        PurgeRequestRepository purgeRequestRepository,
        PurgeRequestQueryService purgeRequestQueryService
    ) {
        this.purgeRequestService = purgeRequestService;
        this.purgeRequestRepository = purgeRequestRepository;
        this.purgeRequestQueryService = purgeRequestQueryService;
    }

    /**
     * {@code POST  /purge-requests} : Create a new purgeRequest.
     *
     * @param purgeRequestDTO the purgeRequestDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new purgeRequestDTO, or with status {@code 400 (Bad Request)} if the purgeRequest has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<PurgeRequestDTO> createPurgeRequest(@Valid @RequestBody PurgeRequestDTO purgeRequestDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save PurgeRequest : {}", purgeRequestDTO);
        if (purgeRequestDTO.getId() != null) {
            throw new BadRequestAlertException("A new purgeRequest cannot already have an ID", ENTITY_NAME, "idexists");
        }
        purgeRequestDTO = purgeRequestService.save(purgeRequestDTO);
        return ResponseEntity.created(new URI("/api/purge-requests/" + purgeRequestDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, purgeRequestDTO.getId().toString()))
            .body(purgeRequestDTO);
    }

    /**
     * {@code PUT  /purge-requests/:id} : Updates an existing purgeRequest.
     *
     * @param id the id of the purgeRequestDTO to save.
     * @param purgeRequestDTO the purgeRequestDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated purgeRequestDTO,
     * or with status {@code 400 (Bad Request)} if the purgeRequestDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the purgeRequestDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<PurgeRequestDTO> updatePurgeRequest(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody PurgeRequestDTO purgeRequestDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update PurgeRequest : {}, {}", id, purgeRequestDTO);
        if (purgeRequestDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, purgeRequestDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!purgeRequestRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        purgeRequestDTO = purgeRequestService.update(purgeRequestDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, purgeRequestDTO.getId().toString()))
            .body(purgeRequestDTO);
    }

    /**
     * {@code PATCH  /purge-requests/:id} : Partial updates given fields of an existing purgeRequest, field will ignore if it is null
     *
     * @param id the id of the purgeRequestDTO to save.
     * @param purgeRequestDTO the purgeRequestDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated purgeRequestDTO,
     * or with status {@code 400 (Bad Request)} if the purgeRequestDTO is not valid,
     * or with status {@code 404 (Not Found)} if the purgeRequestDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the purgeRequestDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<PurgeRequestDTO> partialUpdatePurgeRequest(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody PurgeRequestDTO purgeRequestDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update PurgeRequest partially : {}, {}", id, purgeRequestDTO);
        if (purgeRequestDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, purgeRequestDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!purgeRequestRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<PurgeRequestDTO> result = purgeRequestService.partialUpdate(purgeRequestDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, purgeRequestDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /purge-requests} : get all the purgeRequests.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of purgeRequests in body.
     */
    @GetMapping("")
    public ResponseEntity<List<PurgeRequestDTO>> getAllPurgeRequests(
        PurgeRequestCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get PurgeRequests by criteria: {}", criteria);

        Page<PurgeRequestDTO> page = purgeRequestQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /purge-requests/count} : count all the purgeRequests.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countPurgeRequests(PurgeRequestCriteria criteria) {
        LOG.debug("REST request to count PurgeRequests by criteria: {}", criteria);
        return ResponseEntity.ok().body(purgeRequestQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /purge-requests/:id} : get the "id" purgeRequest.
     *
     * @param id the id of the purgeRequestDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the purgeRequestDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PurgeRequestDTO> getPurgeRequest(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get PurgeRequest : {}", id);
        Optional<PurgeRequestDTO> purgeRequestDTO = purgeRequestService.findOne(id);
        return ResponseUtil.wrapOrNotFound(purgeRequestDTO);
    }

    /**
     * {@code DELETE  /purge-requests/:id} : delete the "id" purgeRequest.
     *
     * @param id the id of the purgeRequestDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePurgeRequest(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete PurgeRequest : {}", id);
        purgeRequestService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
