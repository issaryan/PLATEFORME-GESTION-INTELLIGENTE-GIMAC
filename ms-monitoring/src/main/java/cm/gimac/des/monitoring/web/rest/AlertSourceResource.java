package cm.gimac.des.monitoring.web.rest;

import cm.gimac.des.monitoring.repository.AlertSourceRepository;
import cm.gimac.des.monitoring.service.AlertSourceService;
import cm.gimac.des.monitoring.service.dto.AlertSourceDTO;
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
 * REST controller for managing {@link cm.gimac.des.monitoring.domain.AlertSource}.
 */
@RestController
@RequestMapping("/api/alert-sources")
public class AlertSourceResource {

    private static final Logger LOG = LoggerFactory.getLogger(AlertSourceResource.class);

    private static final String ENTITY_NAME = "monitoringAlertSource";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final AlertSourceService alertSourceService;

    private final AlertSourceRepository alertSourceRepository;

    public AlertSourceResource(AlertSourceService alertSourceService, AlertSourceRepository alertSourceRepository) {
        this.alertSourceService = alertSourceService;
        this.alertSourceRepository = alertSourceRepository;
    }

    /**
     * {@code POST  /alert-sources} : Create a new alertSource.
     *
     * @param alertSourceDTO the alertSourceDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new alertSourceDTO, or with status {@code 400 (Bad Request)} if the alertSource has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<AlertSourceDTO> createAlertSource(@Valid @RequestBody AlertSourceDTO alertSourceDTO) throws URISyntaxException {
        LOG.debug("REST request to save AlertSource : {}", alertSourceDTO);
        if (alertSourceDTO.getId() != null) {
            throw new BadRequestAlertException("A new alertSource cannot already have an ID", ENTITY_NAME, "idexists");
        }
        alertSourceDTO = alertSourceService.save(alertSourceDTO);
        return ResponseEntity.created(new URI("/api/alert-sources/" + alertSourceDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, alertSourceDTO.getId().toString()))
            .body(alertSourceDTO);
    }

    /**
     * {@code PUT  /alert-sources/:id} : Updates an existing alertSource.
     *
     * @param id the id of the alertSourceDTO to save.
     * @param alertSourceDTO the alertSourceDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated alertSourceDTO,
     * or with status {@code 400 (Bad Request)} if the alertSourceDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the alertSourceDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<AlertSourceDTO> updateAlertSource(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody AlertSourceDTO alertSourceDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update AlertSource : {}, {}", id, alertSourceDTO);
        if (alertSourceDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, alertSourceDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!alertSourceRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        alertSourceDTO = alertSourceService.update(alertSourceDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, alertSourceDTO.getId().toString()))
            .body(alertSourceDTO);
    }

    /**
     * {@code PATCH  /alert-sources/:id} : Partial updates given fields of an existing alertSource, field will ignore if it is null
     *
     * @param id the id of the alertSourceDTO to save.
     * @param alertSourceDTO the alertSourceDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated alertSourceDTO,
     * or with status {@code 400 (Bad Request)} if the alertSourceDTO is not valid,
     * or with status {@code 404 (Not Found)} if the alertSourceDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the alertSourceDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<AlertSourceDTO> partialUpdateAlertSource(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody AlertSourceDTO alertSourceDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update AlertSource partially : {}, {}", id, alertSourceDTO);
        if (alertSourceDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, alertSourceDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!alertSourceRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<AlertSourceDTO> result = alertSourceService.partialUpdate(alertSourceDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, alertSourceDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /alert-sources} : get all the alertSources.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of alertSources in body.
     */
    @GetMapping("")
    public List<AlertSourceDTO> getAllAlertSources() {
        LOG.debug("REST request to get all AlertSources");
        return alertSourceService.findAll();
    }

    /**
     * {@code GET  /alert-sources/:id} : get the "id" alertSource.
     *
     * @param id the id of the alertSourceDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the alertSourceDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AlertSourceDTO> getAlertSource(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get AlertSource : {}", id);
        Optional<AlertSourceDTO> alertSourceDTO = alertSourceService.findOne(id);
        return ResponseUtil.wrapOrNotFound(alertSourceDTO);
    }

    /**
     * {@code DELETE  /alert-sources/:id} : delete the "id" alertSource.
     *
     * @param id the id of the alertSourceDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlertSource(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete AlertSource : {}", id);
        alertSourceService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
