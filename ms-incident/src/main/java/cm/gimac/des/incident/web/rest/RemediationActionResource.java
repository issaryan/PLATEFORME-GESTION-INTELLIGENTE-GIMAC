package cm.gimac.des.incident.web.rest;

import cm.gimac.des.incident.repository.RemediationActionRepository;
import cm.gimac.des.incident.service.RemediationActionQueryService;
import cm.gimac.des.incident.service.RemediationActionService;
import cm.gimac.des.incident.service.criteria.RemediationActionCriteria;
import cm.gimac.des.incident.service.dto.RemediationActionDTO;
import cm.gimac.des.incident.web.rest.errors.BadRequestAlertException;
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
 * REST controller for managing {@link cm.gimac.des.incident.domain.RemediationAction}.
 */
@RestController
@RequestMapping("/api/remediation-actions")
public class RemediationActionResource {

    private static final Logger LOG = LoggerFactory.getLogger(RemediationActionResource.class);

    private static final String ENTITY_NAME = "incidentRemediationAction";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final RemediationActionService remediationActionService;

    private final RemediationActionRepository remediationActionRepository;

    private final RemediationActionQueryService remediationActionQueryService;

    public RemediationActionResource(
        RemediationActionService remediationActionService,
        RemediationActionRepository remediationActionRepository,
        RemediationActionQueryService remediationActionQueryService
    ) {
        this.remediationActionService = remediationActionService;
        this.remediationActionRepository = remediationActionRepository;
        this.remediationActionQueryService = remediationActionQueryService;
    }

    /**
     * {@code POST  /remediation-actions} : Create a new remediationAction.
     *
     * @param remediationActionDTO the remediationActionDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new remediationActionDTO, or with status {@code 400 (Bad Request)} if the remediationAction has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<RemediationActionDTO> createRemediationAction(@Valid @RequestBody RemediationActionDTO remediationActionDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save RemediationAction : {}", remediationActionDTO);
        if (remediationActionDTO.getId() != null) {
            throw new BadRequestAlertException("A new remediationAction cannot already have an ID", ENTITY_NAME, "idexists");
        }
        remediationActionDTO = remediationActionService.save(remediationActionDTO);
        return ResponseEntity.created(new URI("/api/remediation-actions/" + remediationActionDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, remediationActionDTO.getId().toString()))
            .body(remediationActionDTO);
    }

    /**
     * {@code PUT  /remediation-actions/:id} : Updates an existing remediationAction.
     *
     * @param id the id of the remediationActionDTO to save.
     * @param remediationActionDTO the remediationActionDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated remediationActionDTO,
     * or with status {@code 400 (Bad Request)} if the remediationActionDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the remediationActionDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<RemediationActionDTO> updateRemediationAction(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody RemediationActionDTO remediationActionDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update RemediationAction : {}, {}", id, remediationActionDTO);
        if (remediationActionDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, remediationActionDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!remediationActionRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        remediationActionDTO = remediationActionService.update(remediationActionDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, remediationActionDTO.getId().toString()))
            .body(remediationActionDTO);
    }

    /**
     * {@code PATCH  /remediation-actions/:id} : Partial updates given fields of an existing remediationAction, field will ignore if it is null
     *
     * @param id the id of the remediationActionDTO to save.
     * @param remediationActionDTO the remediationActionDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated remediationActionDTO,
     * or with status {@code 400 (Bad Request)} if the remediationActionDTO is not valid,
     * or with status {@code 404 (Not Found)} if the remediationActionDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the remediationActionDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<RemediationActionDTO> partialUpdateRemediationAction(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody RemediationActionDTO remediationActionDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update RemediationAction partially : {}, {}", id, remediationActionDTO);
        if (remediationActionDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, remediationActionDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!remediationActionRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<RemediationActionDTO> result = remediationActionService.partialUpdate(remediationActionDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, remediationActionDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /remediation-actions} : get all the remediationActions.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of remediationActions in body.
     */
    @GetMapping("")
    public ResponseEntity<List<RemediationActionDTO>> getAllRemediationActions(
        RemediationActionCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get RemediationActions by criteria: {}", criteria);

        Page<RemediationActionDTO> page = remediationActionQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /remediation-actions/count} : count all the remediationActions.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countRemediationActions(RemediationActionCriteria criteria) {
        LOG.debug("REST request to count RemediationActions by criteria: {}", criteria);
        return ResponseEntity.ok().body(remediationActionQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /remediation-actions/:id} : get the "id" remediationAction.
     *
     * @param id the id of the remediationActionDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the remediationActionDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RemediationActionDTO> getRemediationAction(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get RemediationAction : {}", id);
        Optional<RemediationActionDTO> remediationActionDTO = remediationActionService.findOne(id);
        return ResponseUtil.wrapOrNotFound(remediationActionDTO);
    }

    /**
     * {@code DELETE  /remediation-actions/:id} : delete the "id" remediationAction.
     *
     * @param id the id of the remediationActionDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRemediationAction(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete RemediationAction : {}", id);
        remediationActionService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
