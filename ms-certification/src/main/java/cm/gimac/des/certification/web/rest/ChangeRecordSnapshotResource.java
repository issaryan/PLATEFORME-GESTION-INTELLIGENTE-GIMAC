package cm.gimac.des.certification.web.rest;

import cm.gimac.des.certification.repository.ChangeRecordSnapshotRepository;
import cm.gimac.des.certification.service.ChangeRecordSnapshotService;
import cm.gimac.des.certification.service.dto.ChangeRecordSnapshotDTO;
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
 * REST controller for managing {@link cm.gimac.des.certification.domain.ChangeRecordSnapshot}.
 */
@RestController
@RequestMapping("/api/change-record-snapshots")
public class ChangeRecordSnapshotResource {

    private static final Logger LOG = LoggerFactory.getLogger(ChangeRecordSnapshotResource.class);

    private static final String ENTITY_NAME = "certificationChangeRecordSnapshot";

    @Value("${jhipster.clientApp.name:certification}")
    private String applicationName;

    private final ChangeRecordSnapshotService changeRecordSnapshotService;

    private final ChangeRecordSnapshotRepository changeRecordSnapshotRepository;

    public ChangeRecordSnapshotResource(
        ChangeRecordSnapshotService changeRecordSnapshotService,
        ChangeRecordSnapshotRepository changeRecordSnapshotRepository
    ) {
        this.changeRecordSnapshotService = changeRecordSnapshotService;
        this.changeRecordSnapshotRepository = changeRecordSnapshotRepository;
    }

    /**
     * {@code POST  /change-record-snapshots} : Create a new changeRecordSnapshot.
     *
     * @param changeRecordSnapshotDTO the changeRecordSnapshotDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new changeRecordSnapshotDTO, or with status {@code 400 (Bad Request)} if the changeRecordSnapshot already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<ChangeRecordSnapshotDTO> createChangeRecordSnapshot(
        @Valid @RequestBody ChangeRecordSnapshotDTO changeRecordSnapshotDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to save ChangeRecordSnapshot : {}", changeRecordSnapshotDTO);
        if (changeRecordSnapshotDTO.getId() != null) {
            throw new BadRequestAlertException("A new changeRecordSnapshot cannot already have an ID", ENTITY_NAME, "idexists");
        }
        changeRecordSnapshotDTO = changeRecordSnapshotService.save(changeRecordSnapshotDTO);
        return ResponseEntity.created(new URI("/api/change-record-snapshots/" + changeRecordSnapshotDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, changeRecordSnapshotDTO.getId().toString()))
            .body(changeRecordSnapshotDTO);
    }

    /**
     * {@code PUT  /change-record-snapshots/:id} : Updates an existing changeRecordSnapshot.
     *
     * @param id the id of the changeRecordSnapshotDTO to save.
     * @param changeRecordSnapshotDTO the changeRecordSnapshotDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated changeRecordSnapshotDTO,
     * or with status {@code 400 (Bad Request)} if the changeRecordSnapshotDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the changeRecordSnapshotDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ChangeRecordSnapshotDTO> updateChangeRecordSnapshot(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody ChangeRecordSnapshotDTO changeRecordSnapshotDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update ChangeRecordSnapshot : {}, {}", id, changeRecordSnapshotDTO);
        if (changeRecordSnapshotDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, changeRecordSnapshotDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!changeRecordSnapshotRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        changeRecordSnapshotDTO = changeRecordSnapshotService.update(changeRecordSnapshotDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, changeRecordSnapshotDTO.getId().toString()))
            .body(changeRecordSnapshotDTO);
    }

    /**
     * {@code PATCH  /change-record-snapshots/:id} : Partial updates given fields of an existing changeRecordSnapshot, field will ignore if it is null
     *
     * @param id the id of the changeRecordSnapshotDTO to save.
     * @param changeRecordSnapshotDTO the changeRecordSnapshotDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated changeRecordSnapshotDTO,
     * or with status {@code 400 (Bad Request)} if the changeRecordSnapshotDTO is not valid,
     * or with status {@code 404 (Not Found)} if the changeRecordSnapshotDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the changeRecordSnapshotDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<ChangeRecordSnapshotDTO> partialUpdateChangeRecordSnapshot(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody ChangeRecordSnapshotDTO changeRecordSnapshotDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partially update ChangeRecordSnapshot : {}, {}", id, changeRecordSnapshotDTO);
        if (changeRecordSnapshotDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, changeRecordSnapshotDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!changeRecordSnapshotRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<ChangeRecordSnapshotDTO> result = changeRecordSnapshotService.partialUpdate(changeRecordSnapshotDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, changeRecordSnapshotDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /change-record-snapshots} : get all the Change Record Snapshots.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Change Record Snapshots in body.
     */
    @GetMapping("")
    public List<ChangeRecordSnapshotDTO> getAllChangeRecordSnapshots() {
        LOG.debug("REST request to get all ChangeRecordSnapshots");
        return changeRecordSnapshotService.findAll();
    }

    /**
     * {@code GET  /change-record-snapshots/:id} : get the "id" changeRecordSnapshot.
     *
     * @param id the id of the changeRecordSnapshotDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the changeRecordSnapshotDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ChangeRecordSnapshotDTO> getChangeRecordSnapshot(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get ChangeRecordSnapshot : {}", id);
        Optional<ChangeRecordSnapshotDTO> changeRecordSnapshotDTO = changeRecordSnapshotService.findOne(id);
        return ResponseUtil.wrapOrNotFound(changeRecordSnapshotDTO);
    }

    /**
     * {@code DELETE  /change-record-snapshots/:id} : delete the "id" changeRecordSnapshot.
     *
     * @param id the id of the changeRecordSnapshotDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChangeRecordSnapshot(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete ChangeRecordSnapshot : {}", id);
        changeRecordSnapshotService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
