package cm.gimac.des.certification.web.rest;

import cm.gimac.des.certification.repository.AttestationStatusDecisionRepository;
import cm.gimac.des.certification.service.AttestationStatusDecisionService;
import cm.gimac.des.certification.service.dto.AttestationStatusDecisionDTO;
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
 * REST controller for managing {@link cm.gimac.des.certification.domain.AttestationStatusDecision}.
 */
@RestController
@RequestMapping("/api/attestation-status-decisions")
public class AttestationStatusDecisionResource {

    private static final Logger LOG = LoggerFactory.getLogger(AttestationStatusDecisionResource.class);

    private static final String ENTITY_NAME = "certificationAttestationStatusDecision";

    @Value("${jhipster.clientApp.name:certification}")
    private String applicationName;

    private final AttestationStatusDecisionService attestationStatusDecisionService;

    private final AttestationStatusDecisionRepository attestationStatusDecisionRepository;

    public AttestationStatusDecisionResource(
        AttestationStatusDecisionService attestationStatusDecisionService,
        AttestationStatusDecisionRepository attestationStatusDecisionRepository
    ) {
        this.attestationStatusDecisionService = attestationStatusDecisionService;
        this.attestationStatusDecisionRepository = attestationStatusDecisionRepository;
    }

    /**
     * {@code POST  /attestation-status-decisions} : Create a new attestationStatusDecision.
     *
     * @param attestationStatusDecisionDTO the attestationStatusDecisionDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new attestationStatusDecisionDTO, or with status {@code 400 (Bad Request)} if the attestationStatusDecision already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<AttestationStatusDecisionDTO> createAttestationStatusDecision(
        @Valid @RequestBody AttestationStatusDecisionDTO attestationStatusDecisionDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to save AttestationStatusDecision : {}", attestationStatusDecisionDTO);
        if (attestationStatusDecisionDTO.getId() != null) {
            throw new BadRequestAlertException("A new attestationStatusDecision cannot already have an ID", ENTITY_NAME, "idexists");
        }
        attestationStatusDecisionDTO = attestationStatusDecisionService.save(attestationStatusDecisionDTO);
        return ResponseEntity.created(new URI("/api/attestation-status-decisions/" + attestationStatusDecisionDTO.getId()))
            .headers(
                HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, attestationStatusDecisionDTO.getId().toString())
            )
            .body(attestationStatusDecisionDTO);
    }

    /**
     * {@code PUT  /attestation-status-decisions/:id} : Updates an existing attestationStatusDecision.
     *
     * @param id the id of the attestationStatusDecisionDTO to save.
     * @param attestationStatusDecisionDTO the attestationStatusDecisionDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated attestationStatusDecisionDTO,
     * or with status {@code 400 (Bad Request)} if the attestationStatusDecisionDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the attestationStatusDecisionDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<AttestationStatusDecisionDTO> updateAttestationStatusDecision(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody AttestationStatusDecisionDTO attestationStatusDecisionDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update AttestationStatusDecision : {}, {}", id, attestationStatusDecisionDTO);
        if (attestationStatusDecisionDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, attestationStatusDecisionDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!attestationStatusDecisionRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        attestationStatusDecisionDTO = attestationStatusDecisionService.update(attestationStatusDecisionDTO);
        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, attestationStatusDecisionDTO.getId().toString())
            )
            .body(attestationStatusDecisionDTO);
    }

    /**
     * {@code PATCH  /attestation-status-decisions/:id} : Partial updates given fields of an existing attestationStatusDecision, field will ignore if it is null
     *
     * @param id the id of the attestationStatusDecisionDTO to save.
     * @param attestationStatusDecisionDTO the attestationStatusDecisionDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated attestationStatusDecisionDTO,
     * or with status {@code 400 (Bad Request)} if the attestationStatusDecisionDTO is not valid,
     * or with status {@code 404 (Not Found)} if the attestationStatusDecisionDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the attestationStatusDecisionDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<AttestationStatusDecisionDTO> partialUpdateAttestationStatusDecision(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody AttestationStatusDecisionDTO attestationStatusDecisionDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partially update AttestationStatusDecision : {}, {}", id, attestationStatusDecisionDTO);
        if (attestationStatusDecisionDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, attestationStatusDecisionDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!attestationStatusDecisionRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<AttestationStatusDecisionDTO> result = attestationStatusDecisionService.partialUpdate(attestationStatusDecisionDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, attestationStatusDecisionDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /attestation-status-decisions} : get all the Attestation Status Decisions.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Attestation Status Decisions in body.
     */
    @GetMapping("")
    public List<AttestationStatusDecisionDTO> getAllAttestationStatusDecisions() {
        LOG.debug("REST request to get all AttestationStatusDecisions");
        return attestationStatusDecisionService.findAll();
    }

    /**
     * {@code GET  /attestation-status-decisions/:id} : get the "id" attestationStatusDecision.
     *
     * @param id the id of the attestationStatusDecisionDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the attestationStatusDecisionDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AttestationStatusDecisionDTO> getAttestationStatusDecision(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get AttestationStatusDecision : {}", id);
        Optional<AttestationStatusDecisionDTO> attestationStatusDecisionDTO = attestationStatusDecisionService.findOne(id);
        return ResponseUtil.wrapOrNotFound(attestationStatusDecisionDTO);
    }

    /**
     * {@code DELETE  /attestation-status-decisions/:id} : delete the "id" attestationStatusDecision.
     *
     * @param id the id of the attestationStatusDecisionDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAttestationStatusDecision(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete AttestationStatusDecision : {}", id);
        attestationStatusDecisionService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
