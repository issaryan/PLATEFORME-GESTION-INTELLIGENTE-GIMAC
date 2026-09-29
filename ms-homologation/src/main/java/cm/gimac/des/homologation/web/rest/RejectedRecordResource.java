package cm.gimac.des.homologation.web.rest;

import cm.gimac.des.homologation.repository.RejectedRecordRepository;
import cm.gimac.des.homologation.service.RejectedRecordService;
import cm.gimac.des.homologation.service.dto.RejectedRecordDTO;
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
 * REST controller for managing {@link cm.gimac.des.homologation.domain.RejectedRecord}.
 */
@RestController
@RequestMapping("/api/rejected-records")
public class RejectedRecordResource {

    private static final Logger LOG = LoggerFactory.getLogger(RejectedRecordResource.class);

    private static final String ENTITY_NAME = "homologationRejectedRecord";

    @Value("${jhipster.clientApp.name:homologation}")
    private String applicationName;

    private final RejectedRecordService rejectedRecordService;

    private final RejectedRecordRepository rejectedRecordRepository;

    public RejectedRecordResource(RejectedRecordService rejectedRecordService, RejectedRecordRepository rejectedRecordRepository) {
        this.rejectedRecordService = rejectedRecordService;
        this.rejectedRecordRepository = rejectedRecordRepository;
    }

    /**
     * {@code POST  /rejected-records} : Create a new rejectedRecord.
     *
     * @param rejectedRecordDTO the rejectedRecordDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new rejectedRecordDTO, or with status {@code 400 (Bad Request)} if the rejectedRecord already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<RejectedRecordDTO> createRejectedRecord(@Valid @RequestBody RejectedRecordDTO rejectedRecordDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save RejectedRecord : {}", rejectedRecordDTO);
        if (rejectedRecordDTO.getId() != null) {
            throw new BadRequestAlertException("A new rejectedRecord cannot already have an ID", ENTITY_NAME, "idexists");
        }
        rejectedRecordDTO = rejectedRecordService.save(rejectedRecordDTO);
        return ResponseEntity.created(new URI("/api/rejected-records/" + rejectedRecordDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, rejectedRecordDTO.getId().toString()))
            .body(rejectedRecordDTO);
    }

    /**
     * {@code PUT  /rejected-records/:id} : Updates an existing rejectedRecord.
     *
     * @param id the id of the rejectedRecordDTO to save.
     * @param rejectedRecordDTO the rejectedRecordDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated rejectedRecordDTO,
     * or with status {@code 400 (Bad Request)} if the rejectedRecordDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the rejectedRecordDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<RejectedRecordDTO> updateRejectedRecord(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody RejectedRecordDTO rejectedRecordDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update RejectedRecord : {}, {}", id, rejectedRecordDTO);
        if (rejectedRecordDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, rejectedRecordDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!rejectedRecordRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        rejectedRecordDTO = rejectedRecordService.update(rejectedRecordDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, rejectedRecordDTO.getId().toString()))
            .body(rejectedRecordDTO);
    }

    /**
     * {@code PATCH  /rejected-records/:id} : Partial updates given fields of an existing rejectedRecord, field will ignore if it is null
     *
     * @param id the id of the rejectedRecordDTO to save.
     * @param rejectedRecordDTO the rejectedRecordDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated rejectedRecordDTO,
     * or with status {@code 400 (Bad Request)} if the rejectedRecordDTO is not valid,
     * or with status {@code 404 (Not Found)} if the rejectedRecordDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the rejectedRecordDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<RejectedRecordDTO> partialUpdateRejectedRecord(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody RejectedRecordDTO rejectedRecordDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partially update RejectedRecord : {}, {}", id, rejectedRecordDTO);
        if (rejectedRecordDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, rejectedRecordDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!rejectedRecordRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<RejectedRecordDTO> result = rejectedRecordService.partialUpdate(rejectedRecordDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, rejectedRecordDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /rejected-records} : get all the Rejected Records.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Rejected Records in body.
     */
    @GetMapping("")
    public List<RejectedRecordDTO> getAllRejectedRecords() {
        LOG.debug("REST request to get all RejectedRecords");
        return rejectedRecordService.findAll();
    }

    /**
     * {@code GET  /rejected-records/:id} : get the "id" rejectedRecord.
     *
     * @param id the id of the rejectedRecordDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the rejectedRecordDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RejectedRecordDTO> getRejectedRecord(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get RejectedRecord : {}", id);
        Optional<RejectedRecordDTO> rejectedRecordDTO = rejectedRecordService.findOne(id);
        return ResponseUtil.wrapOrNotFound(rejectedRecordDTO);
    }

    /**
     * {@code DELETE  /rejected-records/:id} : delete the "id" rejectedRecord.
     *
     * @param id the id of the rejectedRecordDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRejectedRecord(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete RejectedRecord : {}", id);
        rejectedRecordService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
