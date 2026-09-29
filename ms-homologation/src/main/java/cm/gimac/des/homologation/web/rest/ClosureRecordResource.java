package cm.gimac.des.homologation.web.rest;

import cm.gimac.des.homologation.repository.ClosureRecordRepository;
import cm.gimac.des.homologation.service.ClosureRecordService;
import cm.gimac.des.homologation.service.dto.ClosureRecordDTO;
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
 * REST controller for managing {@link cm.gimac.des.homologation.domain.ClosureRecord}.
 */
@RestController
@RequestMapping("/api/closure-records")
public class ClosureRecordResource {

    private static final Logger LOG = LoggerFactory.getLogger(ClosureRecordResource.class);

    private static final String ENTITY_NAME = "homologationClosureRecord";

    @Value("${jhipster.clientApp.name:homologation}")
    private String applicationName;

    private final ClosureRecordService closureRecordService;

    private final ClosureRecordRepository closureRecordRepository;

    public ClosureRecordResource(ClosureRecordService closureRecordService, ClosureRecordRepository closureRecordRepository) {
        this.closureRecordService = closureRecordService;
        this.closureRecordRepository = closureRecordRepository;
    }

    /**
     * {@code POST  /closure-records} : Create a new closureRecord.
     *
     * @param closureRecordDTO the closureRecordDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new closureRecordDTO, or with status {@code 400 (Bad Request)} if the closureRecord already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<ClosureRecordDTO> createClosureRecord(@Valid @RequestBody ClosureRecordDTO closureRecordDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save ClosureRecord : {}", closureRecordDTO);
        if (closureRecordDTO.getId() != null) {
            throw new BadRequestAlertException("A new closureRecord cannot already have an ID", ENTITY_NAME, "idexists");
        }
        closureRecordDTO = closureRecordService.save(closureRecordDTO);
        return ResponseEntity.created(new URI("/api/closure-records/" + closureRecordDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, closureRecordDTO.getId().toString()))
            .body(closureRecordDTO);
    }

    /**
     * {@code PUT  /closure-records/:id} : Updates an existing closureRecord.
     *
     * @param id the id of the closureRecordDTO to save.
     * @param closureRecordDTO the closureRecordDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated closureRecordDTO,
     * or with status {@code 400 (Bad Request)} if the closureRecordDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the closureRecordDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ClosureRecordDTO> updateClosureRecord(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody ClosureRecordDTO closureRecordDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update ClosureRecord : {}, {}", id, closureRecordDTO);
        if (closureRecordDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, closureRecordDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!closureRecordRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        closureRecordDTO = closureRecordService.update(closureRecordDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, closureRecordDTO.getId().toString()))
            .body(closureRecordDTO);
    }

    /**
     * {@code PATCH  /closure-records/:id} : Partial updates given fields of an existing closureRecord, field will ignore if it is null
     *
     * @param id the id of the closureRecordDTO to save.
     * @param closureRecordDTO the closureRecordDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated closureRecordDTO,
     * or with status {@code 400 (Bad Request)} if the closureRecordDTO is not valid,
     * or with status {@code 404 (Not Found)} if the closureRecordDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the closureRecordDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<ClosureRecordDTO> partialUpdateClosureRecord(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody ClosureRecordDTO closureRecordDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partially update ClosureRecord : {}, {}", id, closureRecordDTO);
        if (closureRecordDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, closureRecordDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!closureRecordRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<ClosureRecordDTO> result = closureRecordService.partialUpdate(closureRecordDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, closureRecordDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /closure-records} : get all the Closure Records.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Closure Records in body.
     */
    @GetMapping("")
    public List<ClosureRecordDTO> getAllClosureRecords() {
        LOG.debug("REST request to get all ClosureRecords");
        return closureRecordService.findAll();
    }

    /**
     * {@code GET  /closure-records/:id} : get the "id" closureRecord.
     *
     * @param id the id of the closureRecordDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the closureRecordDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ClosureRecordDTO> getClosureRecord(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get ClosureRecord : {}", id);
        Optional<ClosureRecordDTO> closureRecordDTO = closureRecordService.findOne(id);
        return ResponseUtil.wrapOrNotFound(closureRecordDTO);
    }

    /**
     * {@code DELETE  /closure-records/:id} : delete the "id" closureRecord.
     *
     * @param id the id of the closureRecordDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClosureRecord(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete ClosureRecord : {}", id);
        closureRecordService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
