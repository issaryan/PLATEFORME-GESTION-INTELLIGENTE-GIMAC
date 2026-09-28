package cm.gimac.des.incident.web.rest;

import cm.gimac.des.incident.repository.QualificationMatrixRepository;
import cm.gimac.des.incident.service.QualificationMatrixService;
import cm.gimac.des.incident.service.dto.QualificationMatrixDTO;
import cm.gimac.des.incident.web.rest.errors.BadRequestAlertException;
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
 * REST controller for managing {@link cm.gimac.des.incident.domain.QualificationMatrix}.
 */
@RestController
@RequestMapping("/api/qualification-matrices")
public class QualificationMatrixResource {

    private static final Logger LOG = LoggerFactory.getLogger(QualificationMatrixResource.class);

    private static final String ENTITY_NAME = "incidentQualificationMatrix";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final QualificationMatrixService qualificationMatrixService;

    private final QualificationMatrixRepository qualificationMatrixRepository;

    public QualificationMatrixResource(
        QualificationMatrixService qualificationMatrixService,
        QualificationMatrixRepository qualificationMatrixRepository
    ) {
        this.qualificationMatrixService = qualificationMatrixService;
        this.qualificationMatrixRepository = qualificationMatrixRepository;
    }

    /**
     * {@code POST  /qualification-matrices} : Create a new qualificationMatrix.
     *
     * @param qualificationMatrixDTO the qualificationMatrixDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new qualificationMatrixDTO, or with status {@code 400 (Bad Request)} if the qualificationMatrix has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<QualificationMatrixDTO> createQualificationMatrix(
        @Valid @RequestBody QualificationMatrixDTO qualificationMatrixDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to save QualificationMatrix : {}", qualificationMatrixDTO);
        if (qualificationMatrixDTO.getId() != null) {
            throw new BadRequestAlertException("A new qualificationMatrix cannot already have an ID", ENTITY_NAME, "idexists");
        }
        qualificationMatrixDTO = qualificationMatrixService.save(qualificationMatrixDTO);
        return ResponseEntity.created(new URI("/api/qualification-matrices/" + qualificationMatrixDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, qualificationMatrixDTO.getId().toString()))
            .body(qualificationMatrixDTO);
    }

    /**
     * {@code PUT  /qualification-matrices/:id} : Updates an existing qualificationMatrix.
     *
     * @param id the id of the qualificationMatrixDTO to save.
     * @param qualificationMatrixDTO the qualificationMatrixDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated qualificationMatrixDTO,
     * or with status {@code 400 (Bad Request)} if the qualificationMatrixDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the qualificationMatrixDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<QualificationMatrixDTO> updateQualificationMatrix(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody QualificationMatrixDTO qualificationMatrixDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update QualificationMatrix : {}, {}", id, qualificationMatrixDTO);
        if (qualificationMatrixDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, qualificationMatrixDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!qualificationMatrixRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        qualificationMatrixDTO = qualificationMatrixService.update(qualificationMatrixDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, qualificationMatrixDTO.getId().toString()))
            .body(qualificationMatrixDTO);
    }

    /**
     * {@code PATCH  /qualification-matrices/:id} : Partial updates given fields of an existing qualificationMatrix, field will ignore if it is null
     *
     * @param id the id of the qualificationMatrixDTO to save.
     * @param qualificationMatrixDTO the qualificationMatrixDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated qualificationMatrixDTO,
     * or with status {@code 400 (Bad Request)} if the qualificationMatrixDTO is not valid,
     * or with status {@code 404 (Not Found)} if the qualificationMatrixDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the qualificationMatrixDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<QualificationMatrixDTO> partialUpdateQualificationMatrix(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody QualificationMatrixDTO qualificationMatrixDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update QualificationMatrix partially : {}, {}", id, qualificationMatrixDTO);
        if (qualificationMatrixDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, qualificationMatrixDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!qualificationMatrixRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<QualificationMatrixDTO> result = qualificationMatrixService.partialUpdate(qualificationMatrixDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, qualificationMatrixDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /qualification-matrices} : get all the qualificationMatrices.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of qualificationMatrices in body.
     */
    @GetMapping("")
    public List<QualificationMatrixDTO> getAllQualificationMatrices() {
        LOG.debug("REST request to get all QualificationMatrices");
        return qualificationMatrixService.findAll();
    }

    /**
     * {@code GET  /qualification-matrices/:id} : get the "id" qualificationMatrix.
     *
     * @param id the id of the qualificationMatrixDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the qualificationMatrixDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<QualificationMatrixDTO> getQualificationMatrix(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get QualificationMatrix : {}", id);
        Optional<QualificationMatrixDTO> qualificationMatrixDTO = qualificationMatrixService.findOne(id);
        return ResponseUtil.wrapOrNotFound(qualificationMatrixDTO);
    }

    /**
     * {@code DELETE  /qualification-matrices/:id} : delete the "id" qualificationMatrix.
     *
     * @param id the id of the qualificationMatrixDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQualificationMatrix(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete QualificationMatrix : {}", id);
        qualificationMatrixService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
