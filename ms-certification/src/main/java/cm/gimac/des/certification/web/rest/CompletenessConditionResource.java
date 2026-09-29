package cm.gimac.des.certification.web.rest;

import cm.gimac.des.certification.repository.CompletenessConditionRepository;
import cm.gimac.des.certification.service.CompletenessConditionService;
import cm.gimac.des.certification.service.dto.CompletenessConditionDTO;
import cm.gimac.des.certification.web.rest.errors.BadRequestAlertException;
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
 * REST controller for managing {@link cm.gimac.des.certification.domain.CompletenessCondition}.
 */
@RestController
@RequestMapping("/api/completeness-conditions")
public class CompletenessConditionResource {

    private static final Logger LOG = LoggerFactory.getLogger(CompletenessConditionResource.class);

    private static final String ENTITY_NAME = "certificationCompletenessCondition";

    @Value("${jhipster.clientApp.name:certification}")
    private String applicationName;

    private final CompletenessConditionService completenessConditionService;

    private final CompletenessConditionRepository completenessConditionRepository;

    public CompletenessConditionResource(
        CompletenessConditionService completenessConditionService,
        CompletenessConditionRepository completenessConditionRepository
    ) {
        this.completenessConditionService = completenessConditionService;
        this.completenessConditionRepository = completenessConditionRepository;
    }

    /**
     * {@code POST  /completeness-conditions} : Create a new completenessCondition.
     *
     * @param completenessConditionDTO the completenessConditionDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new completenessConditionDTO, or with status {@code 400 (Bad Request)} if the completenessCondition already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<CompletenessConditionDTO> createCompletenessCondition(
        @Valid @RequestBody CompletenessConditionDTO completenessConditionDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to save CompletenessCondition : {}", completenessConditionDTO);
        if (completenessConditionDTO.getId() != null) {
            throw new BadRequestAlertException("A new completenessCondition cannot already have an ID", ENTITY_NAME, "idexists");
        }
        completenessConditionDTO = completenessConditionService.save(completenessConditionDTO);
        return ResponseEntity.created(new URI("/api/completeness-conditions/" + completenessConditionDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, completenessConditionDTO.getId().toString()))
            .body(completenessConditionDTO);
    }

    /**
     * {@code PUT  /completeness-conditions/:id} : Updates an existing completenessCondition.
     *
     * @param id the id of the completenessConditionDTO to save.
     * @param completenessConditionDTO the completenessConditionDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated completenessConditionDTO,
     * or with status {@code 400 (Bad Request)} if the completenessConditionDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the completenessConditionDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<CompletenessConditionDTO> updateCompletenessCondition(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody CompletenessConditionDTO completenessConditionDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update CompletenessCondition : {}, {}", id, completenessConditionDTO);
        if (completenessConditionDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, completenessConditionDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!completenessConditionRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        completenessConditionDTO = completenessConditionService.update(completenessConditionDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, completenessConditionDTO.getId().toString()))
            .body(completenessConditionDTO);
    }

    /**
     * {@code PATCH  /completeness-conditions/:id} : Partial updates given fields of an existing completenessCondition, field will ignore if it is null
     *
     * @param id the id of the completenessConditionDTO to save.
     * @param completenessConditionDTO the completenessConditionDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated completenessConditionDTO,
     * or with status {@code 400 (Bad Request)} if the completenessConditionDTO is not valid,
     * or with status {@code 404 (Not Found)} if the completenessConditionDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the completenessConditionDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<CompletenessConditionDTO> partialUpdateCompletenessCondition(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody CompletenessConditionDTO completenessConditionDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partially update CompletenessCondition : {}, {}", id, completenessConditionDTO);
        if (completenessConditionDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, completenessConditionDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!completenessConditionRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<CompletenessConditionDTO> result = completenessConditionService.partialUpdate(completenessConditionDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, completenessConditionDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /completeness-conditions} : get all the Completeness Conditions.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Completeness Conditions in body.
     */
    @GetMapping("")
    public List<CompletenessConditionDTO> getAllCompletenessConditions() {
        LOG.debug("REST request to get all CompletenessConditions");
        return completenessConditionService.findAll();
    }

    /**
     * {@code GET  /completeness-conditions/:id} : get the "id" completenessCondition.
     *
     * @param id the id of the completenessConditionDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the completenessConditionDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CompletenessConditionDTO> getCompletenessCondition(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get CompletenessCondition : {}", id);
        Optional<CompletenessConditionDTO> completenessConditionDTO = completenessConditionService.findOne(id);
        return ResponseUtil.wrapOrNotFound(completenessConditionDTO);
    }

    /**
     * {@code DELETE  /completeness-conditions/:id} : delete the "id" completenessCondition.
     *
     * @param id the id of the completenessConditionDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCompletenessCondition(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete CompletenessCondition : {}", id);
        completenessConditionService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
