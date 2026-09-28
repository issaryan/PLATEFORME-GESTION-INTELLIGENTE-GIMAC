package cm.gimac.des.reporting.web.rest;

import cm.gimac.des.reporting.repository.ProjectionRuleRepository;
import cm.gimac.des.reporting.service.ProjectionRuleService;
import cm.gimac.des.reporting.service.dto.ProjectionRuleDTO;
import cm.gimac.des.reporting.web.rest.errors.BadRequestAlertException;
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
 * REST controller for managing {@link cm.gimac.des.reporting.domain.ProjectionRule}.
 */
@RestController
@RequestMapping("/api/projection-rules")
public class ProjectionRuleResource {

    private static final Logger LOG = LoggerFactory.getLogger(ProjectionRuleResource.class);

    private static final String ENTITY_NAME = "reportingProjectionRule";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ProjectionRuleService projectionRuleService;

    private final ProjectionRuleRepository projectionRuleRepository;

    public ProjectionRuleResource(ProjectionRuleService projectionRuleService, ProjectionRuleRepository projectionRuleRepository) {
        this.projectionRuleService = projectionRuleService;
        this.projectionRuleRepository = projectionRuleRepository;
    }

    /**
     * {@code POST  /projection-rules} : Create a new projectionRule.
     *
     * @param projectionRuleDTO the projectionRuleDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new projectionRuleDTO, or with status {@code 400 (Bad Request)} if the projectionRule has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<ProjectionRuleDTO> createProjectionRule(@Valid @RequestBody ProjectionRuleDTO projectionRuleDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save ProjectionRule : {}", projectionRuleDTO);
        if (projectionRuleDTO.getId() != null) {
            throw new BadRequestAlertException("A new projectionRule cannot already have an ID", ENTITY_NAME, "idexists");
        }
        projectionRuleDTO = projectionRuleService.save(projectionRuleDTO);
        return ResponseEntity.created(new URI("/api/projection-rules/" + projectionRuleDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, projectionRuleDTO.getId().toString()))
            .body(projectionRuleDTO);
    }

    /**
     * {@code PUT  /projection-rules/:id} : Updates an existing projectionRule.
     *
     * @param id the id of the projectionRuleDTO to save.
     * @param projectionRuleDTO the projectionRuleDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated projectionRuleDTO,
     * or with status {@code 400 (Bad Request)} if the projectionRuleDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the projectionRuleDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProjectionRuleDTO> updateProjectionRule(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody ProjectionRuleDTO projectionRuleDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update ProjectionRule : {}, {}", id, projectionRuleDTO);
        if (projectionRuleDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, projectionRuleDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!projectionRuleRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        projectionRuleDTO = projectionRuleService.update(projectionRuleDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, projectionRuleDTO.getId().toString()))
            .body(projectionRuleDTO);
    }

    /**
     * {@code PATCH  /projection-rules/:id} : Partial updates given fields of an existing projectionRule, field will ignore if it is null
     *
     * @param id the id of the projectionRuleDTO to save.
     * @param projectionRuleDTO the projectionRuleDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated projectionRuleDTO,
     * or with status {@code 400 (Bad Request)} if the projectionRuleDTO is not valid,
     * or with status {@code 404 (Not Found)} if the projectionRuleDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the projectionRuleDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<ProjectionRuleDTO> partialUpdateProjectionRule(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody ProjectionRuleDTO projectionRuleDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update ProjectionRule partially : {}, {}", id, projectionRuleDTO);
        if (projectionRuleDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, projectionRuleDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!projectionRuleRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<ProjectionRuleDTO> result = projectionRuleService.partialUpdate(projectionRuleDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, projectionRuleDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /projection-rules} : get all the projectionRules.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of projectionRules in body.
     */
    @GetMapping("")
    public List<ProjectionRuleDTO> getAllProjectionRules() {
        LOG.debug("REST request to get all ProjectionRules");
        return projectionRuleService.findAll();
    }

    /**
     * {@code GET  /projection-rules/:id} : get the "id" projectionRule.
     *
     * @param id the id of the projectionRuleDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the projectionRuleDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProjectionRuleDTO> getProjectionRule(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get ProjectionRule : {}", id);
        Optional<ProjectionRuleDTO> projectionRuleDTO = projectionRuleService.findOne(id);
        return ResponseUtil.wrapOrNotFound(projectionRuleDTO);
    }

    /**
     * {@code DELETE  /projection-rules/:id} : delete the "id" projectionRule.
     *
     * @param id the id of the projectionRuleDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProjectionRule(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete ProjectionRule : {}", id);
        projectionRuleService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
