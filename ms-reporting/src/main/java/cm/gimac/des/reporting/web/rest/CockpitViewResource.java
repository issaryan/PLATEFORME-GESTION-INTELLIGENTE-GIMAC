package cm.gimac.des.reporting.web.rest;

import cm.gimac.des.reporting.repository.CockpitViewRepository;
import cm.gimac.des.reporting.service.CockpitViewService;
import cm.gimac.des.reporting.service.dto.CockpitViewDTO;
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
 * REST controller for managing {@link cm.gimac.des.reporting.domain.CockpitView}.
 */
@RestController
@RequestMapping("/api/cockpit-views")
public class CockpitViewResource {

    private static final Logger LOG = LoggerFactory.getLogger(CockpitViewResource.class);

    private static final String ENTITY_NAME = "reportingCockpitView";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final CockpitViewService cockpitViewService;

    private final CockpitViewRepository cockpitViewRepository;

    public CockpitViewResource(CockpitViewService cockpitViewService, CockpitViewRepository cockpitViewRepository) {
        this.cockpitViewService = cockpitViewService;
        this.cockpitViewRepository = cockpitViewRepository;
    }

    /**
     * {@code POST  /cockpit-views} : Create a new cockpitView.
     *
     * @param cockpitViewDTO the cockpitViewDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new cockpitViewDTO, or with status {@code 400 (Bad Request)} if the cockpitView has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<CockpitViewDTO> createCockpitView(@Valid @RequestBody CockpitViewDTO cockpitViewDTO) throws URISyntaxException {
        LOG.debug("REST request to save CockpitView : {}", cockpitViewDTO);
        if (cockpitViewDTO.getId() != null) {
            throw new BadRequestAlertException("A new cockpitView cannot already have an ID", ENTITY_NAME, "idexists");
        }
        cockpitViewDTO = cockpitViewService.save(cockpitViewDTO);
        return ResponseEntity.created(new URI("/api/cockpit-views/" + cockpitViewDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, cockpitViewDTO.getId().toString()))
            .body(cockpitViewDTO);
    }

    /**
     * {@code PUT  /cockpit-views/:id} : Updates an existing cockpitView.
     *
     * @param id the id of the cockpitViewDTO to save.
     * @param cockpitViewDTO the cockpitViewDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated cockpitViewDTO,
     * or with status {@code 400 (Bad Request)} if the cockpitViewDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the cockpitViewDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<CockpitViewDTO> updateCockpitView(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody CockpitViewDTO cockpitViewDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update CockpitView : {}, {}", id, cockpitViewDTO);
        if (cockpitViewDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, cockpitViewDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!cockpitViewRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        cockpitViewDTO = cockpitViewService.update(cockpitViewDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, cockpitViewDTO.getId().toString()))
            .body(cockpitViewDTO);
    }

    /**
     * {@code PATCH  /cockpit-views/:id} : Partial updates given fields of an existing cockpitView, field will ignore if it is null
     *
     * @param id the id of the cockpitViewDTO to save.
     * @param cockpitViewDTO the cockpitViewDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated cockpitViewDTO,
     * or with status {@code 400 (Bad Request)} if the cockpitViewDTO is not valid,
     * or with status {@code 404 (Not Found)} if the cockpitViewDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the cockpitViewDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<CockpitViewDTO> partialUpdateCockpitView(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody CockpitViewDTO cockpitViewDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update CockpitView partially : {}, {}", id, cockpitViewDTO);
        if (cockpitViewDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, cockpitViewDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!cockpitViewRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<CockpitViewDTO> result = cockpitViewService.partialUpdate(cockpitViewDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, cockpitViewDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /cockpit-views} : get all the cockpitViews.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of cockpitViews in body.
     */
    @GetMapping("")
    public List<CockpitViewDTO> getAllCockpitViews(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all CockpitViews");
        return cockpitViewService.findAll();
    }

    /**
     * {@code GET  /cockpit-views/:id} : get the "id" cockpitView.
     *
     * @param id the id of the cockpitViewDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the cockpitViewDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CockpitViewDTO> getCockpitView(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get CockpitView : {}", id);
        Optional<CockpitViewDTO> cockpitViewDTO = cockpitViewService.findOne(id);
        return ResponseUtil.wrapOrNotFound(cockpitViewDTO);
    }

    /**
     * {@code DELETE  /cockpit-views/:id} : delete the "id" cockpitView.
     *
     * @param id the id of the cockpitViewDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCockpitView(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete CockpitView : {}", id);
        cockpitViewService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
