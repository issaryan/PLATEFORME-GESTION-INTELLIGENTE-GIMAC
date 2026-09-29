package cm.gimac.des.homologation.web.rest;

import cm.gimac.des.homologation.repository.EvidenceReferenceRepository;
import cm.gimac.des.homologation.service.EvidenceReferenceService;
import cm.gimac.des.homologation.service.dto.EvidenceReferenceDTO;
import cm.gimac.des.homologation.web.rest.errors.BadRequestAlertException;
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
 * REST controller for managing {@link cm.gimac.des.homologation.domain.EvidenceReference}.
 */
@RestController
@RequestMapping("/api/evidence-references")
public class EvidenceReferenceResource {

    private static final Logger LOG = LoggerFactory.getLogger(EvidenceReferenceResource.class);

    private static final String ENTITY_NAME = "homologationEvidenceReference";

    @Value("${jhipster.clientApp.name:homologation}")
    private String applicationName;

    private final EvidenceReferenceService evidenceReferenceService;

    private final EvidenceReferenceRepository evidenceReferenceRepository;

    public EvidenceReferenceResource(
        EvidenceReferenceService evidenceReferenceService,
        EvidenceReferenceRepository evidenceReferenceRepository
    ) {
        this.evidenceReferenceService = evidenceReferenceService;
        this.evidenceReferenceRepository = evidenceReferenceRepository;
    }

    /**
     * {@code POST  /evidence-references} : Create a new evidenceReference.
     *
     * @param evidenceReferenceDTO the evidenceReferenceDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new evidenceReferenceDTO, or with status {@code 400 (Bad Request)} if the evidenceReference already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<EvidenceReferenceDTO> createEvidenceReference(@Valid @RequestBody EvidenceReferenceDTO evidenceReferenceDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save EvidenceReference : {}", evidenceReferenceDTO);
        if (evidenceReferenceDTO.getId() != null) {
            throw new BadRequestAlertException("A new evidenceReference cannot already have an ID", ENTITY_NAME, "idexists");
        }
        evidenceReferenceDTO = evidenceReferenceService.save(evidenceReferenceDTO);
        return ResponseEntity.created(new URI("/api/evidence-references/" + evidenceReferenceDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, evidenceReferenceDTO.getId().toString()))
            .body(evidenceReferenceDTO);
    }

    /**
     * {@code PUT  /evidence-references/:id} : Updates an existing evidenceReference.
     *
     * @param id the id of the evidenceReferenceDTO to save.
     * @param evidenceReferenceDTO the evidenceReferenceDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated evidenceReferenceDTO,
     * or with status {@code 400 (Bad Request)} if the evidenceReferenceDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the evidenceReferenceDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<EvidenceReferenceDTO> updateEvidenceReference(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody EvidenceReferenceDTO evidenceReferenceDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update EvidenceReference : {}, {}", id, evidenceReferenceDTO);
        if (evidenceReferenceDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, evidenceReferenceDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!evidenceReferenceRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        evidenceReferenceDTO = evidenceReferenceService.update(evidenceReferenceDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, evidenceReferenceDTO.getId().toString()))
            .body(evidenceReferenceDTO);
    }

    /**
     * {@code PATCH  /evidence-references/:id} : Partial updates given fields of an existing evidenceReference, field will ignore if it is null
     *
     * @param id the id of the evidenceReferenceDTO to save.
     * @param evidenceReferenceDTO the evidenceReferenceDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated evidenceReferenceDTO,
     * or with status {@code 400 (Bad Request)} if the evidenceReferenceDTO is not valid,
     * or with status {@code 404 (Not Found)} if the evidenceReferenceDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the evidenceReferenceDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<EvidenceReferenceDTO> partialUpdateEvidenceReference(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody EvidenceReferenceDTO evidenceReferenceDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partially update EvidenceReference : {}, {}", id, evidenceReferenceDTO);
        if (evidenceReferenceDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, evidenceReferenceDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!evidenceReferenceRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<EvidenceReferenceDTO> result = evidenceReferenceService.partialUpdate(evidenceReferenceDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, evidenceReferenceDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /evidence-references} : get all the Evidence References.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Evidence References in body.
     */
    @GetMapping("")
    public List<EvidenceReferenceDTO> getAllEvidenceReferences() {
        LOG.debug("REST request to get all EvidenceReferences");
        return evidenceReferenceService.findAll();
    }

    /**
     * {@code GET  /evidence-references/:id} : get the "id" evidenceReference.
     *
     * @param id the id of the evidenceReferenceDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the evidenceReferenceDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<EvidenceReferenceDTO> getEvidenceReference(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get EvidenceReference : {}", id);
        Optional<EvidenceReferenceDTO> evidenceReferenceDTO = evidenceReferenceService.findOne(id);
        return ResponseUtil.wrapOrNotFound(evidenceReferenceDTO);
    }

    /**
     * {@code DELETE  /evidence-references/:id} : delete the "id" evidenceReference.
     *
     * @param id the id of the evidenceReferenceDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvidenceReference(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete EvidenceReference : {}", id);
        evidenceReferenceService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
