package cm.gimac.des.pilotage.web.rest;

import cm.gimac.des.pilotage.repository.AcceptanceCriterionRepository;
import cm.gimac.des.pilotage.service.AcceptanceCriterionQueryService;
import cm.gimac.des.pilotage.service.AcceptanceCriterionService;
import cm.gimac.des.pilotage.service.criteria.AcceptanceCriterionCriteria;
import cm.gimac.des.pilotage.service.dto.AcceptanceCriterionDTO;
import cm.gimac.des.pilotage.web.rest.errors.BadRequestAlertException;
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
 * REST controller for managing {@link cm.gimac.des.pilotage.domain.AcceptanceCriterion}.
 */
@RestController
@RequestMapping("/api/acceptance-criteria")
public class AcceptanceCriterionResource {

    private static final Logger LOG = LoggerFactory.getLogger(AcceptanceCriterionResource.class);

    private static final String ENTITY_NAME = "pilotageAcceptanceCriterion";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final AcceptanceCriterionService acceptanceCriterionService;

    private final AcceptanceCriterionRepository acceptanceCriterionRepository;

    private final AcceptanceCriterionQueryService acceptanceCriterionQueryService;

    public AcceptanceCriterionResource(
        AcceptanceCriterionService acceptanceCriterionService,
        AcceptanceCriterionRepository acceptanceCriterionRepository,
        AcceptanceCriterionQueryService acceptanceCriterionQueryService
    ) {
        this.acceptanceCriterionService = acceptanceCriterionService;
        this.acceptanceCriterionRepository = acceptanceCriterionRepository;
        this.acceptanceCriterionQueryService = acceptanceCriterionQueryService;
    }

    /**
     * {@code POST  /acceptance-criteria} : Create a new acceptanceCriterion.
     *
     * @param acceptanceCriterionDTO the acceptanceCriterionDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new acceptanceCriterionDTO, or with status {@code 400 (Bad Request)} if the acceptanceCriterion has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<AcceptanceCriterionDTO> createAcceptanceCriterion(
        @Valid @RequestBody AcceptanceCriterionDTO acceptanceCriterionDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to save AcceptanceCriterion : {}", acceptanceCriterionDTO);
        if (acceptanceCriterionDTO.getId() != null) {
            throw new BadRequestAlertException("A new acceptanceCriterion cannot already have an ID", ENTITY_NAME, "idexists");
        }
        acceptanceCriterionDTO = acceptanceCriterionService.save(acceptanceCriterionDTO);
        return ResponseEntity.created(new URI("/api/acceptance-criteria/" + acceptanceCriterionDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, acceptanceCriterionDTO.getId().toString()))
            .body(acceptanceCriterionDTO);
    }

    /**
     * {@code PUT  /acceptance-criteria/:id} : Updates an existing acceptanceCriterion.
     *
     * @param id the id of the acceptanceCriterionDTO to save.
     * @param acceptanceCriterionDTO the acceptanceCriterionDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated acceptanceCriterionDTO,
     * or with status {@code 400 (Bad Request)} if the acceptanceCriterionDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the acceptanceCriterionDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<AcceptanceCriterionDTO> updateAcceptanceCriterion(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody AcceptanceCriterionDTO acceptanceCriterionDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update AcceptanceCriterion : {}, {}", id, acceptanceCriterionDTO);
        if (acceptanceCriterionDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, acceptanceCriterionDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!acceptanceCriterionRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        acceptanceCriterionDTO = acceptanceCriterionService.update(acceptanceCriterionDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, acceptanceCriterionDTO.getId().toString()))
            .body(acceptanceCriterionDTO);
    }

    /**
     * {@code PATCH  /acceptance-criteria/:id} : Partial updates given fields of an existing acceptanceCriterion, field will ignore if it is null
     *
     * @param id the id of the acceptanceCriterionDTO to save.
     * @param acceptanceCriterionDTO the acceptanceCriterionDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated acceptanceCriterionDTO,
     * or with status {@code 400 (Bad Request)} if the acceptanceCriterionDTO is not valid,
     * or with status {@code 404 (Not Found)} if the acceptanceCriterionDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the acceptanceCriterionDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<AcceptanceCriterionDTO> partialUpdateAcceptanceCriterion(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody AcceptanceCriterionDTO acceptanceCriterionDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update AcceptanceCriterion partially : {}, {}", id, acceptanceCriterionDTO);
        if (acceptanceCriterionDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, acceptanceCriterionDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!acceptanceCriterionRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<AcceptanceCriterionDTO> result = acceptanceCriterionService.partialUpdate(acceptanceCriterionDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, acceptanceCriterionDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /acceptance-criteria} : get all the acceptanceCriteria.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of acceptanceCriteria in body.
     */
    @GetMapping("")
    public ResponseEntity<List<AcceptanceCriterionDTO>> getAllAcceptanceCriteria(AcceptanceCriterionCriteria criteria) {
        LOG.debug("REST request to get AcceptanceCriteria by criteria: {}", criteria);

        List<AcceptanceCriterionDTO> entityList = acceptanceCriterionQueryService.findByCriteria(criteria);
        return ResponseEntity.ok().body(entityList);
    }

    /**
     * {@code GET  /acceptance-criteria/count} : count all the acceptanceCriteria.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countAcceptanceCriteria(AcceptanceCriterionCriteria criteria) {
        LOG.debug("REST request to count AcceptanceCriteria by criteria: {}", criteria);
        return ResponseEntity.ok().body(acceptanceCriterionQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /acceptance-criteria/:id} : get the "id" acceptanceCriterion.
     *
     * @param id the id of the acceptanceCriterionDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the acceptanceCriterionDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AcceptanceCriterionDTO> getAcceptanceCriterion(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get AcceptanceCriterion : {}", id);
        Optional<AcceptanceCriterionDTO> acceptanceCriterionDTO = acceptanceCriterionService.findOne(id);
        return ResponseUtil.wrapOrNotFound(acceptanceCriterionDTO);
    }

    /**
     * {@code DELETE  /acceptance-criteria/:id} : delete the "id" acceptanceCriterion.
     *
     * @param id the id of the acceptanceCriterionDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAcceptanceCriterion(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete AcceptanceCriterion : {}", id);
        acceptanceCriterionService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
