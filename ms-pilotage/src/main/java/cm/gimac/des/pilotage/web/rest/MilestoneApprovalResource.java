package cm.gimac.des.pilotage.web.rest;

import cm.gimac.des.pilotage.repository.MilestoneApprovalRepository;
import cm.gimac.des.pilotage.service.MilestoneApprovalService;
import cm.gimac.des.pilotage.service.dto.MilestoneApprovalDTO;
import cm.gimac.des.pilotage.web.rest.errors.BadRequestAlertException;
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
 * REST controller for managing {@link cm.gimac.des.pilotage.domain.MilestoneApproval}.
 */
@RestController
@RequestMapping("/api/milestone-approvals")
public class MilestoneApprovalResource {

    private static final Logger LOG = LoggerFactory.getLogger(MilestoneApprovalResource.class);

    private static final String ENTITY_NAME = "pilotageMilestoneApproval";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final MilestoneApprovalService milestoneApprovalService;

    private final MilestoneApprovalRepository milestoneApprovalRepository;

    public MilestoneApprovalResource(
        MilestoneApprovalService milestoneApprovalService,
        MilestoneApprovalRepository milestoneApprovalRepository
    ) {
        this.milestoneApprovalService = milestoneApprovalService;
        this.milestoneApprovalRepository = milestoneApprovalRepository;
    }

    /**
     * {@code POST  /milestone-approvals} : Create a new milestoneApproval.
     *
     * @param milestoneApprovalDTO the milestoneApprovalDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new milestoneApprovalDTO, or with status {@code 400 (Bad Request)} if the milestoneApproval has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<MilestoneApprovalDTO> createMilestoneApproval(@Valid @RequestBody MilestoneApprovalDTO milestoneApprovalDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save MilestoneApproval : {}", milestoneApprovalDTO);
        if (milestoneApprovalDTO.getId() != null) {
            throw new BadRequestAlertException("A new milestoneApproval cannot already have an ID", ENTITY_NAME, "idexists");
        }
        milestoneApprovalDTO = milestoneApprovalService.save(milestoneApprovalDTO);
        return ResponseEntity.created(new URI("/api/milestone-approvals/" + milestoneApprovalDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, milestoneApprovalDTO.getId().toString()))
            .body(milestoneApprovalDTO);
    }

    /**
     * {@code PUT  /milestone-approvals/:id} : Updates an existing milestoneApproval.
     *
     * @param id the id of the milestoneApprovalDTO to save.
     * @param milestoneApprovalDTO the milestoneApprovalDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated milestoneApprovalDTO,
     * or with status {@code 400 (Bad Request)} if the milestoneApprovalDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the milestoneApprovalDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<MilestoneApprovalDTO> updateMilestoneApproval(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody MilestoneApprovalDTO milestoneApprovalDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update MilestoneApproval : {}, {}", id, milestoneApprovalDTO);
        if (milestoneApprovalDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, milestoneApprovalDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!milestoneApprovalRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        milestoneApprovalDTO = milestoneApprovalService.update(milestoneApprovalDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, milestoneApprovalDTO.getId().toString()))
            .body(milestoneApprovalDTO);
    }

    /**
     * {@code PATCH  /milestone-approvals/:id} : Partial updates given fields of an existing milestoneApproval, field will ignore if it is null
     *
     * @param id the id of the milestoneApprovalDTO to save.
     * @param milestoneApprovalDTO the milestoneApprovalDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated milestoneApprovalDTO,
     * or with status {@code 400 (Bad Request)} if the milestoneApprovalDTO is not valid,
     * or with status {@code 404 (Not Found)} if the milestoneApprovalDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the milestoneApprovalDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<MilestoneApprovalDTO> partialUpdateMilestoneApproval(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody MilestoneApprovalDTO milestoneApprovalDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update MilestoneApproval partially : {}, {}", id, milestoneApprovalDTO);
        if (milestoneApprovalDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, milestoneApprovalDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!milestoneApprovalRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<MilestoneApprovalDTO> result = milestoneApprovalService.partialUpdate(milestoneApprovalDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, milestoneApprovalDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /milestone-approvals} : get all the milestoneApprovals.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of milestoneApprovals in body.
     */
    @GetMapping("")
    public List<MilestoneApprovalDTO> getAllMilestoneApprovals(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all MilestoneApprovals");
        return milestoneApprovalService.findAll();
    }

    /**
     * {@code GET  /milestone-approvals/:id} : get the "id" milestoneApproval.
     *
     * @param id the id of the milestoneApprovalDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the milestoneApprovalDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<MilestoneApprovalDTO> getMilestoneApproval(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get MilestoneApproval : {}", id);
        Optional<MilestoneApprovalDTO> milestoneApprovalDTO = milestoneApprovalService.findOne(id);
        return ResponseUtil.wrapOrNotFound(milestoneApprovalDTO);
    }

    /**
     * {@code DELETE  /milestone-approvals/:id} : delete the "id" milestoneApproval.
     *
     * @param id the id of the milestoneApprovalDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMilestoneApproval(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete MilestoneApproval : {}", id);
        milestoneApprovalService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
