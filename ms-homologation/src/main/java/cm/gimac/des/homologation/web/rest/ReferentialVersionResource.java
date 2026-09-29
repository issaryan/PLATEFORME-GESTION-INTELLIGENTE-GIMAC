package cm.gimac.des.homologation.web.rest;

import cm.gimac.des.homologation.repository.ReferentialVersionRepository;
import cm.gimac.des.homologation.service.ReferentialVersionService;
import cm.gimac.des.homologation.service.dto.ReferentialVersionDTO;
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
 * REST controller for managing {@link cm.gimac.des.homologation.domain.ReferentialVersion}.
 */
@RestController
@RequestMapping("/api/referential-versions")
public class ReferentialVersionResource {

    private static final Logger LOG = LoggerFactory.getLogger(ReferentialVersionResource.class);

    private static final String ENTITY_NAME = "homologationReferentialVersion";

    @Value("${jhipster.clientApp.name:homologation}")
    private String applicationName;

    private final ReferentialVersionService referentialVersionService;

    private final ReferentialVersionRepository referentialVersionRepository;

    public ReferentialVersionResource(
        ReferentialVersionService referentialVersionService,
        ReferentialVersionRepository referentialVersionRepository
    ) {
        this.referentialVersionService = referentialVersionService;
        this.referentialVersionRepository = referentialVersionRepository;
    }

    /**
     * {@code POST  /referential-versions} : Create a new referentialVersion.
     *
     * @param referentialVersionDTO the referentialVersionDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new referentialVersionDTO, or with status {@code 400 (Bad Request)} if the referentialVersion already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<ReferentialVersionDTO> createReferentialVersion(@Valid @RequestBody ReferentialVersionDTO referentialVersionDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save ReferentialVersion : {}", referentialVersionDTO);
        if (referentialVersionDTO.getId() != null) {
            throw new BadRequestAlertException("A new referentialVersion cannot already have an ID", ENTITY_NAME, "idexists");
        }
        referentialVersionDTO = referentialVersionService.save(referentialVersionDTO);
        return ResponseEntity.created(new URI("/api/referential-versions/" + referentialVersionDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, referentialVersionDTO.getId().toString()))
            .body(referentialVersionDTO);
    }

    /**
     * {@code PUT  /referential-versions/:id} : Updates an existing referentialVersion.
     *
     * @param id the id of the referentialVersionDTO to save.
     * @param referentialVersionDTO the referentialVersionDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated referentialVersionDTO,
     * or with status {@code 400 (Bad Request)} if the referentialVersionDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the referentialVersionDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ReferentialVersionDTO> updateReferentialVersion(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody ReferentialVersionDTO referentialVersionDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update ReferentialVersion : {}, {}", id, referentialVersionDTO);
        if (referentialVersionDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, referentialVersionDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!referentialVersionRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        referentialVersionDTO = referentialVersionService.update(referentialVersionDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, referentialVersionDTO.getId().toString()))
            .body(referentialVersionDTO);
    }

    /**
     * {@code PATCH  /referential-versions/:id} : Partial updates given fields of an existing referentialVersion, field will ignore if it is null
     *
     * @param id the id of the referentialVersionDTO to save.
     * @param referentialVersionDTO the referentialVersionDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated referentialVersionDTO,
     * or with status {@code 400 (Bad Request)} if the referentialVersionDTO is not valid,
     * or with status {@code 404 (Not Found)} if the referentialVersionDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the referentialVersionDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<ReferentialVersionDTO> partialUpdateReferentialVersion(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody ReferentialVersionDTO referentialVersionDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partially update ReferentialVersion : {}, {}", id, referentialVersionDTO);
        if (referentialVersionDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, referentialVersionDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!referentialVersionRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<ReferentialVersionDTO> result = referentialVersionService.partialUpdate(referentialVersionDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, referentialVersionDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /referential-versions} : get all the Referential Versions.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Referential Versions in body.
     */
    @GetMapping("")
    public List<ReferentialVersionDTO> getAllReferentialVersions() {
        LOG.debug("REST request to get all ReferentialVersions");
        return referentialVersionService.findAll();
    }

    /**
     * {@code GET  /referential-versions/:id} : get the "id" referentialVersion.
     *
     * @param id the id of the referentialVersionDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the referentialVersionDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReferentialVersionDTO> getReferentialVersion(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get ReferentialVersion : {}", id);
        Optional<ReferentialVersionDTO> referentialVersionDTO = referentialVersionService.findOne(id);
        return ResponseUtil.wrapOrNotFound(referentialVersionDTO);
    }

    /**
     * {@code DELETE  /referential-versions/:id} : delete the "id" referentialVersion.
     *
     * @param id the id of the referentialVersionDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReferentialVersion(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete ReferentialVersion : {}", id);
        referentialVersionService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
