package cm.gimac.des.monitoring.web.rest;

import cm.gimac.des.monitoring.repository.AlertSeverityPolicyRepository;
import cm.gimac.des.monitoring.service.AlertSeverityPolicyService;
import cm.gimac.des.monitoring.service.dto.AlertSeverityPolicyDTO;
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
 * REST controller for managing {@link cm.gimac.des.monitoring.domain.AlertSeverityPolicy}.
 */
@RestController
@RequestMapping("/api/alert-severity-policies")
public class AlertSeverityPolicyResource {

    private static final Logger LOG = LoggerFactory.getLogger(AlertSeverityPolicyResource.class);

    private static final String ENTITY_NAME = "monitoringAlertSeverityPolicy";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final AlertSeverityPolicyService alertSeverityPolicyService;

    private final AlertSeverityPolicyRepository alertSeverityPolicyRepository;

    public AlertSeverityPolicyResource(
        AlertSeverityPolicyService alertSeverityPolicyService,
        AlertSeverityPolicyRepository alertSeverityPolicyRepository
    ) {
        this.alertSeverityPolicyService = alertSeverityPolicyService;
        this.alertSeverityPolicyRepository = alertSeverityPolicyRepository;
    }

    /**
     * {@code POST  /alert-severity-policies} : Create a new alertSeverityPolicy.
     *
     * @param alertSeverityPolicyDTO the alertSeverityPolicyDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new alertSeverityPolicyDTO, or with status {@code 400 (Bad Request)} if the alertSeverityPolicy has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<AlertSeverityPolicyDTO> createAlertSeverityPolicy(
        @Valid @RequestBody AlertSeverityPolicyDTO alertSeverityPolicyDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to save AlertSeverityPolicy : {}", alertSeverityPolicyDTO);
        if (alertSeverityPolicyDTO.getId() != null) {
            throw new BadRequestAlertException("A new alertSeverityPolicy cannot already have an ID", ENTITY_NAME, "idexists");
        }
        alertSeverityPolicyDTO = alertSeverityPolicyService.save(alertSeverityPolicyDTO);
        return ResponseEntity.created(new URI("/api/alert-severity-policies/" + alertSeverityPolicyDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, alertSeverityPolicyDTO.getId().toString()))
            .body(alertSeverityPolicyDTO);
    }

    /**
     * {@code PUT  /alert-severity-policies/:id} : Updates an existing alertSeverityPolicy.
     *
     * @param id the id of the alertSeverityPolicyDTO to save.
     * @param alertSeverityPolicyDTO the alertSeverityPolicyDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated alertSeverityPolicyDTO,
     * or with status {@code 400 (Bad Request)} if the alertSeverityPolicyDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the alertSeverityPolicyDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<AlertSeverityPolicyDTO> updateAlertSeverityPolicy(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody AlertSeverityPolicyDTO alertSeverityPolicyDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update AlertSeverityPolicy : {}, {}", id, alertSeverityPolicyDTO);
        if (alertSeverityPolicyDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, alertSeverityPolicyDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!alertSeverityPolicyRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        alertSeverityPolicyDTO = alertSeverityPolicyService.update(alertSeverityPolicyDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, alertSeverityPolicyDTO.getId().toString()))
            .body(alertSeverityPolicyDTO);
    }

    /**
     * {@code PATCH  /alert-severity-policies/:id} : Partial updates given fields of an existing alertSeverityPolicy, field will ignore if it is null
     *
     * @param id the id of the alertSeverityPolicyDTO to save.
     * @param alertSeverityPolicyDTO the alertSeverityPolicyDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated alertSeverityPolicyDTO,
     * or with status {@code 400 (Bad Request)} if the alertSeverityPolicyDTO is not valid,
     * or with status {@code 404 (Not Found)} if the alertSeverityPolicyDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the alertSeverityPolicyDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<AlertSeverityPolicyDTO> partialUpdateAlertSeverityPolicy(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody AlertSeverityPolicyDTO alertSeverityPolicyDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update AlertSeverityPolicy partially : {}, {}", id, alertSeverityPolicyDTO);
        if (alertSeverityPolicyDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, alertSeverityPolicyDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!alertSeverityPolicyRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<AlertSeverityPolicyDTO> result = alertSeverityPolicyService.partialUpdate(alertSeverityPolicyDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, alertSeverityPolicyDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /alert-severity-policies} : get all the alertSeverityPolicies.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of alertSeverityPolicies in body.
     */
    @GetMapping("")
    public List<AlertSeverityPolicyDTO> getAllAlertSeverityPolicies() {
        LOG.debug("REST request to get all AlertSeverityPolicies");
        return alertSeverityPolicyService.findAll();
    }

    /**
     * {@code GET  /alert-severity-policies/:id} : get the "id" alertSeverityPolicy.
     *
     * @param id the id of the alertSeverityPolicyDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the alertSeverityPolicyDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AlertSeverityPolicyDTO> getAlertSeverityPolicy(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get AlertSeverityPolicy : {}", id);
        Optional<AlertSeverityPolicyDTO> alertSeverityPolicyDTO = alertSeverityPolicyService.findOne(id);
        return ResponseUtil.wrapOrNotFound(alertSeverityPolicyDTO);
    }

    /**
     * {@code DELETE  /alert-severity-policies/:id} : delete the "id" alertSeverityPolicy.
     *
     * @param id the id of the alertSeverityPolicyDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlertSeverityPolicy(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete AlertSeverityPolicy : {}", id);
        alertSeverityPolicyService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
