package cm.gimac.des.iam.web.rest;

import cm.gimac.des.iam.repository.AccessReviewDecisionRepository;
import cm.gimac.des.iam.service.AccessReviewDecisionService;
import cm.gimac.des.iam.service.dto.AccessReviewDecisionDTO;
import cm.gimac.des.iam.web.rest.errors.BadRequestAlertException;
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
 * REST controller for managing {@link cm.gimac.des.iam.domain.AccessReviewDecision}.
 */
@RestController
@RequestMapping("/api/access-review-decisions")
public class AccessReviewDecisionResource {

    private static final Logger LOG = LoggerFactory.getLogger(AccessReviewDecisionResource.class);

    private static final String ENTITY_NAME = "iamAccessReviewDecision";

    @Value("${jhipster.clientApp.name:iam}")
    private String applicationName;

    private final AccessReviewDecisionService accessReviewDecisionService;

    private final AccessReviewDecisionRepository accessReviewDecisionRepository;

    public AccessReviewDecisionResource(
        AccessReviewDecisionService accessReviewDecisionService,
        AccessReviewDecisionRepository accessReviewDecisionRepository
    ) {
        this.accessReviewDecisionService = accessReviewDecisionService;
        this.accessReviewDecisionRepository = accessReviewDecisionRepository;
    }

    /**
     * {@code POST  /access-review-decisions} : Create a new accessReviewDecision.
     *
     * @param accessReviewDecisionDTO the accessReviewDecisionDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new accessReviewDecisionDTO, or with status {@code 400 (Bad Request)} if the accessReviewDecision already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<AccessReviewDecisionDTO> createAccessReviewDecision(
        @Valid @RequestBody AccessReviewDecisionDTO accessReviewDecisionDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to save AccessReviewDecision : {}", accessReviewDecisionDTO);
        if (accessReviewDecisionDTO.getId() != null) {
            throw new BadRequestAlertException("A new accessReviewDecision cannot already have an ID", ENTITY_NAME, "idexists");
        }
        accessReviewDecisionDTO = accessReviewDecisionService.save(accessReviewDecisionDTO);
        return ResponseEntity.created(new URI("/api/access-review-decisions/" + accessReviewDecisionDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, accessReviewDecisionDTO.getId().toString()))
            .body(accessReviewDecisionDTO);
    }

    /**
     * {@code PUT  /access-review-decisions/:id} : Updates an existing accessReviewDecision.
     *
     * @param id the id of the accessReviewDecisionDTO to save.
     * @param accessReviewDecisionDTO the accessReviewDecisionDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated accessReviewDecisionDTO,
     * or with status {@code 400 (Bad Request)} if the accessReviewDecisionDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the accessReviewDecisionDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<AccessReviewDecisionDTO> updateAccessReviewDecision(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody AccessReviewDecisionDTO accessReviewDecisionDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update AccessReviewDecision : {}, {}", id, accessReviewDecisionDTO);
        if (accessReviewDecisionDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, accessReviewDecisionDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!accessReviewDecisionRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        accessReviewDecisionDTO = accessReviewDecisionService.update(accessReviewDecisionDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, accessReviewDecisionDTO.getId().toString()))
            .body(accessReviewDecisionDTO);
    }

    /**
     * {@code PATCH  /access-review-decisions/:id} : Partial updates given fields of an existing accessReviewDecision, field will ignore if it is null
     *
     * @param id the id of the accessReviewDecisionDTO to save.
     * @param accessReviewDecisionDTO the accessReviewDecisionDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated accessReviewDecisionDTO,
     * or with status {@code 400 (Bad Request)} if the accessReviewDecisionDTO is not valid,
     * or with status {@code 404 (Not Found)} if the accessReviewDecisionDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the accessReviewDecisionDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<AccessReviewDecisionDTO> partialUpdateAccessReviewDecision(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody AccessReviewDecisionDTO accessReviewDecisionDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partially update AccessReviewDecision : {}, {}", id, accessReviewDecisionDTO);
        if (accessReviewDecisionDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, accessReviewDecisionDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!accessReviewDecisionRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<AccessReviewDecisionDTO> result = accessReviewDecisionService.partialUpdate(accessReviewDecisionDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, accessReviewDecisionDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /access-review-decisions} : get all the Access Review Decisions.
     *
     * @param pageable the pagination information.
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Access Review Decisions in body.
     */
    @GetMapping("")
    public ResponseEntity<List<AccessReviewDecisionDTO>> getAllAccessReviewDecisions(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get a page of AccessReviewDecisions");
        Page<AccessReviewDecisionDTO> page;
        if (eagerload) {
            page = accessReviewDecisionService.findAllWithEagerRelationships(pageable);
        } else {
            page = accessReviewDecisionService.findAll(pageable);
        }
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /access-review-decisions/:id} : get the "id" accessReviewDecision.
     *
     * @param id the id of the accessReviewDecisionDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the accessReviewDecisionDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AccessReviewDecisionDTO> getAccessReviewDecision(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get AccessReviewDecision : {}", id);
        Optional<AccessReviewDecisionDTO> accessReviewDecisionDTO = accessReviewDecisionService.findOne(id);
        return ResponseUtil.wrapOrNotFound(accessReviewDecisionDTO);
    }

    /**
     * {@code DELETE  /access-review-decisions/:id} : delete the "id" accessReviewDecision.
     *
     * @param id the id of the accessReviewDecisionDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccessReviewDecision(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete AccessReviewDecision : {}", id);
        accessReviewDecisionService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
