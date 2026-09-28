package cm.gimac.des.monitoring.web.rest;

import cm.gimac.des.monitoring.repository.ThresholdRepository;
import cm.gimac.des.monitoring.service.ThresholdService;
import cm.gimac.des.monitoring.service.dto.ThresholdDTO;
import cm.gimac.des.monitoring.web.rest.errors.BadRequestAlertException;
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
 * REST controller for managing {@link cm.gimac.des.monitoring.domain.Threshold}.
 */
@RestController
@RequestMapping("/api/thresholds")
public class ThresholdResource {

    private static final Logger LOG = LoggerFactory.getLogger(ThresholdResource.class);

    private static final String ENTITY_NAME = "monitoringThreshold";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ThresholdService thresholdService;

    private final ThresholdRepository thresholdRepository;

    public ThresholdResource(ThresholdService thresholdService, ThresholdRepository thresholdRepository) {
        this.thresholdService = thresholdService;
        this.thresholdRepository = thresholdRepository;
    }

    /**
     * {@code POST  /thresholds} : Create a new threshold.
     *
     * @param thresholdDTO the thresholdDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new thresholdDTO, or with status {@code 400 (Bad Request)} if the threshold has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<ThresholdDTO> createThreshold(@Valid @RequestBody ThresholdDTO thresholdDTO) throws URISyntaxException {
        LOG.debug("REST request to save Threshold : {}", thresholdDTO);
        if (thresholdDTO.getId() != null) {
            throw new BadRequestAlertException("A new threshold cannot already have an ID", ENTITY_NAME, "idexists");
        }
        thresholdDTO = thresholdService.save(thresholdDTO);
        return ResponseEntity.created(new URI("/api/thresholds/" + thresholdDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, thresholdDTO.getId().toString()))
            .body(thresholdDTO);
    }

    /**
     * {@code PUT  /thresholds/:id} : Updates an existing threshold.
     *
     * @param id the id of the thresholdDTO to save.
     * @param thresholdDTO the thresholdDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated thresholdDTO,
     * or with status {@code 400 (Bad Request)} if the thresholdDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the thresholdDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ThresholdDTO> updateThreshold(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody ThresholdDTO thresholdDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Threshold : {}, {}", id, thresholdDTO);
        if (thresholdDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, thresholdDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!thresholdRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        thresholdDTO = thresholdService.update(thresholdDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, thresholdDTO.getId().toString()))
            .body(thresholdDTO);
    }

    /**
     * {@code PATCH  /thresholds/:id} : Partial updates given fields of an existing threshold, field will ignore if it is null
     *
     * @param id the id of the thresholdDTO to save.
     * @param thresholdDTO the thresholdDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated thresholdDTO,
     * or with status {@code 400 (Bad Request)} if the thresholdDTO is not valid,
     * or with status {@code 404 (Not Found)} if the thresholdDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the thresholdDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<ThresholdDTO> partialUpdateThreshold(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody ThresholdDTO thresholdDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Threshold partially : {}, {}", id, thresholdDTO);
        if (thresholdDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, thresholdDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!thresholdRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<ThresholdDTO> result = thresholdService.partialUpdate(thresholdDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, thresholdDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /thresholds} : get all the thresholds.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of thresholds in body.
     */
    @GetMapping("")
    public List<ThresholdDTO> getAllThresholds(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all Thresholds");
        return thresholdService.findAll();
    }

    /**
     * {@code GET  /thresholds/:id} : get the "id" threshold.
     *
     * @param id the id of the thresholdDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the thresholdDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ThresholdDTO> getThreshold(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get Threshold : {}", id);
        Optional<ThresholdDTO> thresholdDTO = thresholdService.findOne(id);
        return ResponseUtil.wrapOrNotFound(thresholdDTO);
    }

    /**
     * {@code DELETE  /thresholds/:id} : delete the "id" threshold.
     *
     * @param id the id of the thresholdDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteThreshold(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete Threshold : {}", id);
        thresholdService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
