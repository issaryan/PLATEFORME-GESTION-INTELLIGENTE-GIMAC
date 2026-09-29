package cm.gimac.des.homologation.web.rest;

import cm.gimac.des.homologation.repository.CoverageRepository;
import cm.gimac.des.homologation.service.CoverageService;
import cm.gimac.des.homologation.service.dto.CoverageDTO;
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
 * REST controller for managing {@link cm.gimac.des.homologation.domain.Coverage}.
 */
@RestController
@RequestMapping("/api/coverages")
public class CoverageResource {

    private static final Logger LOG = LoggerFactory.getLogger(CoverageResource.class);

    private static final String ENTITY_NAME = "homologationCoverage";

    @Value("${jhipster.clientApp.name:homologation}")
    private String applicationName;

    private final CoverageService coverageService;

    private final CoverageRepository coverageRepository;

    public CoverageResource(CoverageService coverageService, CoverageRepository coverageRepository) {
        this.coverageService = coverageService;
        this.coverageRepository = coverageRepository;
    }

    /**
     * {@code POST  /coverages} : Create a new coverage.
     *
     * @param coverageDTO the coverageDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new coverageDTO, or with status {@code 400 (Bad Request)} if the coverage already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<CoverageDTO> createCoverage(@Valid @RequestBody CoverageDTO coverageDTO) throws URISyntaxException {
        LOG.debug("REST request to save Coverage : {}", coverageDTO);
        if (coverageDTO.getId() != null) {
            throw new BadRequestAlertException("A new coverage cannot already have an ID", ENTITY_NAME, "idexists");
        }
        coverageDTO = coverageService.save(coverageDTO);
        return ResponseEntity.created(new URI("/api/coverages/" + coverageDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, coverageDTO.getId().toString()))
            .body(coverageDTO);
    }

    /**
     * {@code PUT  /coverages/:id} : Updates an existing coverage.
     *
     * @param id the id of the coverageDTO to save.
     * @param coverageDTO the coverageDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated coverageDTO,
     * or with status {@code 400 (Bad Request)} if the coverageDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the coverageDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<CoverageDTO> updateCoverage(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody CoverageDTO coverageDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Coverage : {}, {}", id, coverageDTO);
        if (coverageDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, coverageDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!coverageRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        coverageDTO = coverageService.update(coverageDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, coverageDTO.getId().toString()))
            .body(coverageDTO);
    }

    /**
     * {@code PATCH  /coverages/:id} : Partial updates given fields of an existing coverage, field will ignore if it is null
     *
     * @param id the id of the coverageDTO to save.
     * @param coverageDTO the coverageDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated coverageDTO,
     * or with status {@code 400 (Bad Request)} if the coverageDTO is not valid,
     * or with status {@code 404 (Not Found)} if the coverageDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the coverageDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<CoverageDTO> partialUpdateCoverage(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody CoverageDTO coverageDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partially update Coverage : {}, {}", id, coverageDTO);
        if (coverageDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, coverageDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!coverageRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<CoverageDTO> result = coverageService.partialUpdate(coverageDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, coverageDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /coverages} : get all the Coverages.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Coverages in body.
     */
    @GetMapping("")
    public List<CoverageDTO> getAllCoverages() {
        LOG.debug("REST request to get all Coverages");
        return coverageService.findAll();
    }

    /**
     * {@code GET  /coverages/:id} : get the "id" coverage.
     *
     * @param id the id of the coverageDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the coverageDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CoverageDTO> getCoverage(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get Coverage : {}", id);
        Optional<CoverageDTO> coverageDTO = coverageService.findOne(id);
        return ResponseUtil.wrapOrNotFound(coverageDTO);
    }

    /**
     * {@code DELETE  /coverages/:id} : delete the "id" coverage.
     *
     * @param id the id of the coverageDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCoverage(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete Coverage : {}", id);
        coverageService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
