package cm.gimac.des.pilotage.web.rest;

import cm.gimac.des.pilotage.repository.DecisionRepository;
import cm.gimac.des.pilotage.service.DecisionQueryService;
import cm.gimac.des.pilotage.service.DecisionService;
import cm.gimac.des.pilotage.service.criteria.DecisionCriteria;
import cm.gimac.des.pilotage.service.dto.DecisionDTO;
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
 * REST controller for managing {@link cm.gimac.des.pilotage.domain.Decision}.
 */
@RestController
@RequestMapping("/api/decisions")
public class DecisionResource {

    private static final Logger LOG = LoggerFactory.getLogger(DecisionResource.class);

    private static final String ENTITY_NAME = "pilotageDecision";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final DecisionService decisionService;

    private final DecisionRepository decisionRepository;

    private final DecisionQueryService decisionQueryService;

    public DecisionResource(
        DecisionService decisionService,
        DecisionRepository decisionRepository,
        DecisionQueryService decisionQueryService
    ) {
        this.decisionService = decisionService;
        this.decisionRepository = decisionRepository;
        this.decisionQueryService = decisionQueryService;
    }

    /**
     * {@code POST  /decisions} : Create a new decision.
     *
     * @param decisionDTO the decisionDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new decisionDTO, or with status {@code 400 (Bad Request)} if the decision has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<DecisionDTO> createDecision(@Valid @RequestBody DecisionDTO decisionDTO) throws URISyntaxException {
        LOG.debug("REST request to save Decision : {}", decisionDTO);
        if (decisionDTO.getId() != null) {
            throw new BadRequestAlertException("A new decision cannot already have an ID", ENTITY_NAME, "idexists");
        }
        decisionDTO = decisionService.save(decisionDTO);
        return ResponseEntity.created(new URI("/api/decisions/" + decisionDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, decisionDTO.getId().toString()))
            .body(decisionDTO);
    }

    /**
     * {@code PUT  /decisions/:id} : Updates an existing decision.
     *
     * @param id the id of the decisionDTO to save.
     * @param decisionDTO the decisionDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated decisionDTO,
     * or with status {@code 400 (Bad Request)} if the decisionDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the decisionDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<DecisionDTO> updateDecision(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody DecisionDTO decisionDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Decision : {}, {}", id, decisionDTO);
        if (decisionDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, decisionDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!decisionRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        decisionDTO = decisionService.update(decisionDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, decisionDTO.getId().toString()))
            .body(decisionDTO);
    }

    /**
     * {@code PATCH  /decisions/:id} : Partial updates given fields of an existing decision, field will ignore if it is null
     *
     * @param id the id of the decisionDTO to save.
     * @param decisionDTO the decisionDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated decisionDTO,
     * or with status {@code 400 (Bad Request)} if the decisionDTO is not valid,
     * or with status {@code 404 (Not Found)} if the decisionDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the decisionDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<DecisionDTO> partialUpdateDecision(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody DecisionDTO decisionDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Decision partially : {}, {}", id, decisionDTO);
        if (decisionDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, decisionDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!decisionRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<DecisionDTO> result = decisionService.partialUpdate(decisionDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, decisionDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /decisions} : get all the decisions.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of decisions in body.
     */
    @GetMapping("")
    public ResponseEntity<List<DecisionDTO>> getAllDecisions(
        DecisionCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get Decisions by criteria: {}", criteria);

        Page<DecisionDTO> page = decisionQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /decisions/count} : count all the decisions.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countDecisions(DecisionCriteria criteria) {
        LOG.debug("REST request to count Decisions by criteria: {}", criteria);
        return ResponseEntity.ok().body(decisionQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /decisions/:id} : get the "id" decision.
     *
     * @param id the id of the decisionDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the decisionDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DecisionDTO> getDecision(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get Decision : {}", id);
        Optional<DecisionDTO> decisionDTO = decisionService.findOne(id);
        return ResponseUtil.wrapOrNotFound(decisionDTO);
    }

    /**
     * {@code DELETE  /decisions/:id} : delete the "id" decision.
     *
     * @param id the id of the decisionDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDecision(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete Decision : {}", id);
        decisionService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
