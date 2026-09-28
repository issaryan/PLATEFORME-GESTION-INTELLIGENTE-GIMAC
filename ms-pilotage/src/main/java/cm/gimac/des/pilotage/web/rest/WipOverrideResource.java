package cm.gimac.des.pilotage.web.rest;

import cm.gimac.des.pilotage.repository.WipOverrideRepository;
import cm.gimac.des.pilotage.service.WipOverrideQueryService;
import cm.gimac.des.pilotage.service.WipOverrideService;
import cm.gimac.des.pilotage.service.criteria.WipOverrideCriteria;
import cm.gimac.des.pilotage.service.dto.WipOverrideDTO;
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
 * REST controller for managing {@link cm.gimac.des.pilotage.domain.WipOverride}.
 */
@RestController
@RequestMapping("/api/wip-overrides")
public class WipOverrideResource {

    private static final Logger LOG = LoggerFactory.getLogger(WipOverrideResource.class);

    private static final String ENTITY_NAME = "pilotageWipOverride";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final WipOverrideService wipOverrideService;

    private final WipOverrideRepository wipOverrideRepository;

    private final WipOverrideQueryService wipOverrideQueryService;

    public WipOverrideResource(
        WipOverrideService wipOverrideService,
        WipOverrideRepository wipOverrideRepository,
        WipOverrideQueryService wipOverrideQueryService
    ) {
        this.wipOverrideService = wipOverrideService;
        this.wipOverrideRepository = wipOverrideRepository;
        this.wipOverrideQueryService = wipOverrideQueryService;
    }

    /**
     * {@code POST  /wip-overrides} : Create a new wipOverride.
     *
     * @param wipOverrideDTO the wipOverrideDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new wipOverrideDTO, or with status {@code 400 (Bad Request)} if the wipOverride has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<WipOverrideDTO> createWipOverride(@Valid @RequestBody WipOverrideDTO wipOverrideDTO) throws URISyntaxException {
        LOG.debug("REST request to save WipOverride : {}", wipOverrideDTO);
        if (wipOverrideDTO.getId() != null) {
            throw new BadRequestAlertException("A new wipOverride cannot already have an ID", ENTITY_NAME, "idexists");
        }
        wipOverrideDTO = wipOverrideService.save(wipOverrideDTO);
        return ResponseEntity.created(new URI("/api/wip-overrides/" + wipOverrideDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, wipOverrideDTO.getId().toString()))
            .body(wipOverrideDTO);
    }

    /**
     * {@code PUT  /wip-overrides/:id} : Updates an existing wipOverride.
     *
     * @param id the id of the wipOverrideDTO to save.
     * @param wipOverrideDTO the wipOverrideDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated wipOverrideDTO,
     * or with status {@code 400 (Bad Request)} if the wipOverrideDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the wipOverrideDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<WipOverrideDTO> updateWipOverride(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody WipOverrideDTO wipOverrideDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update WipOverride : {}, {}", id, wipOverrideDTO);
        if (wipOverrideDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, wipOverrideDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!wipOverrideRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        wipOverrideDTO = wipOverrideService.update(wipOverrideDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, wipOverrideDTO.getId().toString()))
            .body(wipOverrideDTO);
    }

    /**
     * {@code PATCH  /wip-overrides/:id} : Partial updates given fields of an existing wipOverride, field will ignore if it is null
     *
     * @param id the id of the wipOverrideDTO to save.
     * @param wipOverrideDTO the wipOverrideDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated wipOverrideDTO,
     * or with status {@code 400 (Bad Request)} if the wipOverrideDTO is not valid,
     * or with status {@code 404 (Not Found)} if the wipOverrideDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the wipOverrideDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<WipOverrideDTO> partialUpdateWipOverride(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody WipOverrideDTO wipOverrideDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update WipOverride partially : {}, {}", id, wipOverrideDTO);
        if (wipOverrideDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, wipOverrideDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!wipOverrideRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<WipOverrideDTO> result = wipOverrideService.partialUpdate(wipOverrideDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, wipOverrideDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /wip-overrides} : get all the wipOverrides.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of wipOverrides in body.
     */
    @GetMapping("")
    public ResponseEntity<List<WipOverrideDTO>> getAllWipOverrides(
        WipOverrideCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get WipOverrides by criteria: {}", criteria);

        Page<WipOverrideDTO> page = wipOverrideQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /wip-overrides/count} : count all the wipOverrides.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countWipOverrides(WipOverrideCriteria criteria) {
        LOG.debug("REST request to count WipOverrides by criteria: {}", criteria);
        return ResponseEntity.ok().body(wipOverrideQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /wip-overrides/:id} : get the "id" wipOverride.
     *
     * @param id the id of the wipOverrideDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the wipOverrideDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<WipOverrideDTO> getWipOverride(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get WipOverride : {}", id);
        Optional<WipOverrideDTO> wipOverrideDTO = wipOverrideService.findOne(id);
        return ResponseUtil.wrapOrNotFound(wipOverrideDTO);
    }

    /**
     * {@code DELETE  /wip-overrides/:id} : delete the "id" wipOverride.
     *
     * @param id the id of the wipOverrideDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWipOverride(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete WipOverride : {}", id);
        wipOverrideService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
