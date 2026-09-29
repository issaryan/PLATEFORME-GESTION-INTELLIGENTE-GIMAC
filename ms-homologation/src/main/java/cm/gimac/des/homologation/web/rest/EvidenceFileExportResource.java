package cm.gimac.des.homologation.web.rest;

import cm.gimac.des.homologation.repository.EvidenceFileExportRepository;
import cm.gimac.des.homologation.service.EvidenceFileExportService;
import cm.gimac.des.homologation.service.dto.EvidenceFileExportDTO;
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
 * REST controller for managing {@link cm.gimac.des.homologation.domain.EvidenceFileExport}.
 */
@RestController
@RequestMapping("/api/evidence-file-exports")
public class EvidenceFileExportResource {

    private static final Logger LOG = LoggerFactory.getLogger(EvidenceFileExportResource.class);

    private static final String ENTITY_NAME = "homologationEvidenceFileExport";

    @Value("${jhipster.clientApp.name:homologation}")
    private String applicationName;

    private final EvidenceFileExportService evidenceFileExportService;

    private final EvidenceFileExportRepository evidenceFileExportRepository;

    public EvidenceFileExportResource(
        EvidenceFileExportService evidenceFileExportService,
        EvidenceFileExportRepository evidenceFileExportRepository
    ) {
        this.evidenceFileExportService = evidenceFileExportService;
        this.evidenceFileExportRepository = evidenceFileExportRepository;
    }

    /**
     * {@code POST  /evidence-file-exports} : Create a new evidenceFileExport.
     *
     * @param evidenceFileExportDTO the evidenceFileExportDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new evidenceFileExportDTO, or with status {@code 400 (Bad Request)} if the evidenceFileExport already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<EvidenceFileExportDTO> createEvidenceFileExport(@Valid @RequestBody EvidenceFileExportDTO evidenceFileExportDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save EvidenceFileExport : {}", evidenceFileExportDTO);
        if (evidenceFileExportDTO.getId() != null) {
            throw new BadRequestAlertException("A new evidenceFileExport cannot already have an ID", ENTITY_NAME, "idexists");
        }
        evidenceFileExportDTO = evidenceFileExportService.save(evidenceFileExportDTO);
        return ResponseEntity.created(new URI("/api/evidence-file-exports/" + evidenceFileExportDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, evidenceFileExportDTO.getId().toString()))
            .body(evidenceFileExportDTO);
    }

    /**
     * {@code PUT  /evidence-file-exports/:id} : Updates an existing evidenceFileExport.
     *
     * @param id the id of the evidenceFileExportDTO to save.
     * @param evidenceFileExportDTO the evidenceFileExportDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated evidenceFileExportDTO,
     * or with status {@code 400 (Bad Request)} if the evidenceFileExportDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the evidenceFileExportDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<EvidenceFileExportDTO> updateEvidenceFileExport(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody EvidenceFileExportDTO evidenceFileExportDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update EvidenceFileExport : {}, {}", id, evidenceFileExportDTO);
        if (evidenceFileExportDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, evidenceFileExportDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!evidenceFileExportRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        evidenceFileExportDTO = evidenceFileExportService.update(evidenceFileExportDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, evidenceFileExportDTO.getId().toString()))
            .body(evidenceFileExportDTO);
    }

    /**
     * {@code PATCH  /evidence-file-exports/:id} : Partial updates given fields of an existing evidenceFileExport, field will ignore if it is null
     *
     * @param id the id of the evidenceFileExportDTO to save.
     * @param evidenceFileExportDTO the evidenceFileExportDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated evidenceFileExportDTO,
     * or with status {@code 400 (Bad Request)} if the evidenceFileExportDTO is not valid,
     * or with status {@code 404 (Not Found)} if the evidenceFileExportDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the evidenceFileExportDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<EvidenceFileExportDTO> partialUpdateEvidenceFileExport(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody EvidenceFileExportDTO evidenceFileExportDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partially update EvidenceFileExport : {}, {}", id, evidenceFileExportDTO);
        if (evidenceFileExportDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, evidenceFileExportDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!evidenceFileExportRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<EvidenceFileExportDTO> result = evidenceFileExportService.partialUpdate(evidenceFileExportDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, evidenceFileExportDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /evidence-file-exports} : get all the Evidence File Exports.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Evidence File Exports in body.
     */
    @GetMapping("")
    public List<EvidenceFileExportDTO> getAllEvidenceFileExports() {
        LOG.debug("REST request to get all EvidenceFileExports");
        return evidenceFileExportService.findAll();
    }

    /**
     * {@code GET  /evidence-file-exports/:id} : get the "id" evidenceFileExport.
     *
     * @param id the id of the evidenceFileExportDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the evidenceFileExportDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<EvidenceFileExportDTO> getEvidenceFileExport(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get EvidenceFileExport : {}", id);
        Optional<EvidenceFileExportDTO> evidenceFileExportDTO = evidenceFileExportService.findOne(id);
        return ResponseUtil.wrapOrNotFound(evidenceFileExportDTO);
    }

    /**
     * {@code DELETE  /evidence-file-exports/:id} : delete the "id" evidenceFileExport.
     *
     * @param id the id of the evidenceFileExportDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvidenceFileExport(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete EvidenceFileExport : {}", id);
        evidenceFileExportService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
