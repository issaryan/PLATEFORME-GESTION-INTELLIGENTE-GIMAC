package cm.gimac.des.reporting.web.rest;

import cm.gimac.des.reporting.repository.IndicatorDefinitionRepository;
import cm.gimac.des.reporting.service.IndicatorDefinitionQueryService;
import cm.gimac.des.reporting.service.IndicatorDefinitionService;
import cm.gimac.des.reporting.service.criteria.IndicatorDefinitionCriteria;
import cm.gimac.des.reporting.service.dto.IndicatorDefinitionDTO;
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
 * REST controller for managing {@link cm.gimac.des.reporting.domain.IndicatorDefinition}.
 */
@RestController
@RequestMapping("/api/indicator-definitions")
public class IndicatorDefinitionResource {

    private static final Logger LOG = LoggerFactory.getLogger(IndicatorDefinitionResource.class);

    private static final String ENTITY_NAME = "reportingIndicatorDefinition";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final IndicatorDefinitionService indicatorDefinitionService;

    private final IndicatorDefinitionRepository indicatorDefinitionRepository;

    private final IndicatorDefinitionQueryService indicatorDefinitionQueryService;

    public IndicatorDefinitionResource(
        IndicatorDefinitionService indicatorDefinitionService,
        IndicatorDefinitionRepository indicatorDefinitionRepository,
        IndicatorDefinitionQueryService indicatorDefinitionQueryService
    ) {
        this.indicatorDefinitionService = indicatorDefinitionService;
        this.indicatorDefinitionRepository = indicatorDefinitionRepository;
        this.indicatorDefinitionQueryService = indicatorDefinitionQueryService;
    }

    /**
     * {@code POST  /indicator-definitions} : Create a new indicatorDefinition.
     *
     * @param indicatorDefinitionDTO the indicatorDefinitionDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new indicatorDefinitionDTO, or with status {@code 400 (Bad Request)} if the indicatorDefinition has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<IndicatorDefinitionDTO> createIndicatorDefinition(
        @Valid @RequestBody IndicatorDefinitionDTO indicatorDefinitionDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to save IndicatorDefinition : {}", indicatorDefinitionDTO);
        if (indicatorDefinitionDTO.getId() != null) {
            throw new BadRequestAlertException("A new indicatorDefinition cannot already have an ID", ENTITY_NAME, "idexists");
        }
        indicatorDefinitionDTO = indicatorDefinitionService.save(indicatorDefinitionDTO);
        return ResponseEntity.created(new URI("/api/indicator-definitions/" + indicatorDefinitionDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, indicatorDefinitionDTO.getId().toString()))
            .body(indicatorDefinitionDTO);
    }

    /**
     * {@code PUT  /indicator-definitions/:id} : Updates an existing indicatorDefinition.
     *
     * @param id the id of the indicatorDefinitionDTO to save.
     * @param indicatorDefinitionDTO the indicatorDefinitionDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated indicatorDefinitionDTO,
     * or with status {@code 400 (Bad Request)} if the indicatorDefinitionDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the indicatorDefinitionDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<IndicatorDefinitionDTO> updateIndicatorDefinition(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody IndicatorDefinitionDTO indicatorDefinitionDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update IndicatorDefinition : {}, {}", id, indicatorDefinitionDTO);
        if (indicatorDefinitionDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, indicatorDefinitionDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!indicatorDefinitionRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        indicatorDefinitionDTO = indicatorDefinitionService.update(indicatorDefinitionDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, indicatorDefinitionDTO.getId().toString()))
            .body(indicatorDefinitionDTO);
    }

    /**
     * {@code PATCH  /indicator-definitions/:id} : Partial updates given fields of an existing indicatorDefinition, field will ignore if it is null
     *
     * @param id the id of the indicatorDefinitionDTO to save.
     * @param indicatorDefinitionDTO the indicatorDefinitionDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated indicatorDefinitionDTO,
     * or with status {@code 400 (Bad Request)} if the indicatorDefinitionDTO is not valid,
     * or with status {@code 404 (Not Found)} if the indicatorDefinitionDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the indicatorDefinitionDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<IndicatorDefinitionDTO> partialUpdateIndicatorDefinition(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody IndicatorDefinitionDTO indicatorDefinitionDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update IndicatorDefinition partially : {}, {}", id, indicatorDefinitionDTO);
        if (indicatorDefinitionDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, indicatorDefinitionDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!indicatorDefinitionRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<IndicatorDefinitionDTO> result = indicatorDefinitionService.partialUpdate(indicatorDefinitionDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, indicatorDefinitionDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /indicator-definitions} : get all the indicatorDefinitions.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of indicatorDefinitions in body.
     */
    @GetMapping("")
    public ResponseEntity<List<IndicatorDefinitionDTO>> getAllIndicatorDefinitions(IndicatorDefinitionCriteria criteria) {
        LOG.debug("REST request to get IndicatorDefinitions by criteria: {}", criteria);

        List<IndicatorDefinitionDTO> entityList = indicatorDefinitionQueryService.findByCriteria(criteria);
        return ResponseEntity.ok().body(entityList);
    }

    /**
     * {@code GET  /indicator-definitions/count} : count all the indicatorDefinitions.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countIndicatorDefinitions(IndicatorDefinitionCriteria criteria) {
        LOG.debug("REST request to count IndicatorDefinitions by criteria: {}", criteria);
        return ResponseEntity.ok().body(indicatorDefinitionQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /indicator-definitions/:id} : get the "id" indicatorDefinition.
     *
     * @param id the id of the indicatorDefinitionDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the indicatorDefinitionDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<IndicatorDefinitionDTO> getIndicatorDefinition(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get IndicatorDefinition : {}", id);
        Optional<IndicatorDefinitionDTO> indicatorDefinitionDTO = indicatorDefinitionService.findOne(id);
        return ResponseUtil.wrapOrNotFound(indicatorDefinitionDTO);
    }

    /**
     * {@code DELETE  /indicator-definitions/:id} : delete the "id" indicatorDefinition.
     *
     * @param id the id of the indicatorDefinitionDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIndicatorDefinition(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete IndicatorDefinition : {}", id);
        indicatorDefinitionService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
