package cm.gimac.des.homologation.web.rest;

import cm.gimac.des.homologation.repository.ReferentialMigrationRepository;
import cm.gimac.des.homologation.service.ReferentialMigrationService;
import cm.gimac.des.homologation.service.dto.ReferentialMigrationDTO;
import cm.gimac.des.homologation.web.rest.errors.BadRequestAlertException;
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
 * REST controller for managing {@link cm.gimac.des.homologation.domain.ReferentialMigration}.
 */
@RestController
@RequestMapping("/api/referential-migrations")
public class ReferentialMigrationResource {

    private static final Logger LOG = LoggerFactory.getLogger(ReferentialMigrationResource.class);

    private static final String ENTITY_NAME = "homologationReferentialMigration";

    @Value("${jhipster.clientApp.name:homologation}")
    private String applicationName;

    private final ReferentialMigrationService referentialMigrationService;

    private final ReferentialMigrationRepository referentialMigrationRepository;

    public ReferentialMigrationResource(
        ReferentialMigrationService referentialMigrationService,
        ReferentialMigrationRepository referentialMigrationRepository
    ) {
        this.referentialMigrationService = referentialMigrationService;
        this.referentialMigrationRepository = referentialMigrationRepository;
    }

    /**
     * {@code POST  /referential-migrations} : Create a new referentialMigration.
     *
     * @param referentialMigrationDTO the referentialMigrationDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new referentialMigrationDTO, or with status {@code 400 (Bad Request)} if the referentialMigration already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<ReferentialMigrationDTO> createReferentialMigration(@RequestBody ReferentialMigrationDTO referentialMigrationDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save ReferentialMigration : {}", referentialMigrationDTO);
        if (referentialMigrationDTO.getId() != null) {
            throw new BadRequestAlertException("A new referentialMigration cannot already have an ID", ENTITY_NAME, "idexists");
        }
        referentialMigrationDTO = referentialMigrationService.save(referentialMigrationDTO);
        return ResponseEntity.created(new URI("/api/referential-migrations/" + referentialMigrationDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, referentialMigrationDTO.getId().toString()))
            .body(referentialMigrationDTO);
    }

    /**
     * {@code PUT  /referential-migrations/:id} : Updates an existing referentialMigration.
     *
     * @param id the id of the referentialMigrationDTO to save.
     * @param referentialMigrationDTO the referentialMigrationDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated referentialMigrationDTO,
     * or with status {@code 400 (Bad Request)} if the referentialMigrationDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the referentialMigrationDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ReferentialMigrationDTO> updateReferentialMigration(
        @PathVariable(value = "id", required = false) final UUID id,
        @RequestBody ReferentialMigrationDTO referentialMigrationDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update ReferentialMigration : {}, {}", id, referentialMigrationDTO);
        if (referentialMigrationDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, referentialMigrationDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!referentialMigrationRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        referentialMigrationDTO = referentialMigrationService.update(referentialMigrationDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, referentialMigrationDTO.getId().toString()))
            .body(referentialMigrationDTO);
    }

    /**
     * {@code PATCH  /referential-migrations/:id} : Partial updates given fields of an existing referentialMigration, field will ignore if it is null
     *
     * @param id the id of the referentialMigrationDTO to save.
     * @param referentialMigrationDTO the referentialMigrationDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated referentialMigrationDTO,
     * or with status {@code 400 (Bad Request)} if the referentialMigrationDTO is not valid,
     * or with status {@code 404 (Not Found)} if the referentialMigrationDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the referentialMigrationDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<ReferentialMigrationDTO> partialUpdateReferentialMigration(
        @PathVariable(value = "id", required = false) final UUID id,
        @RequestBody ReferentialMigrationDTO referentialMigrationDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partially update ReferentialMigration : {}, {}", id, referentialMigrationDTO);
        if (referentialMigrationDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, referentialMigrationDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!referentialMigrationRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<ReferentialMigrationDTO> result = referentialMigrationService.partialUpdate(referentialMigrationDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, referentialMigrationDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /referential-migrations} : get all the Referential Migrations.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Referential Migrations in body.
     */
    @GetMapping("")
    public List<ReferentialMigrationDTO> getAllReferentialMigrations() {
        LOG.debug("REST request to get all ReferentialMigrations");
        return referentialMigrationService.findAll();
    }

    /**
     * {@code GET  /referential-migrations/:id} : get the "id" referentialMigration.
     *
     * @param id the id of the referentialMigrationDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the referentialMigrationDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReferentialMigrationDTO> getReferentialMigration(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get ReferentialMigration : {}", id);
        Optional<ReferentialMigrationDTO> referentialMigrationDTO = referentialMigrationService.findOne(id);
        return ResponseUtil.wrapOrNotFound(referentialMigrationDTO);
    }

    /**
     * {@code DELETE  /referential-migrations/:id} : delete the "id" referentialMigration.
     *
     * @param id the id of the referentialMigrationDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReferentialMigration(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete ReferentialMigration : {}", id);
        referentialMigrationService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
