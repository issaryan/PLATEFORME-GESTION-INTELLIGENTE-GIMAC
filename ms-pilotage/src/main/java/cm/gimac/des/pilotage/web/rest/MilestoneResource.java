package cm.gimac.des.pilotage.web.rest;

import cm.gimac.des.pilotage.repository.MilestoneRepository;
import cm.gimac.des.pilotage.service.MilestoneQueryService;
import cm.gimac.des.pilotage.service.MilestoneService;
import cm.gimac.des.pilotage.service.criteria.MilestoneCriteria;
import cm.gimac.des.pilotage.service.dto.MilestoneDTO;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link cm.gimac.des.pilotage.domain.Milestone}.
 */
@RestController
@RequestMapping("/api/milestones")
public class MilestoneResource {

    private static final Logger LOG = LoggerFactory.getLogger(MilestoneResource.class);

    private static final String ENTITY_NAME = "pilotageMilestone";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final MilestoneService milestoneService;

    private final MilestoneRepository milestoneRepository;

    private final MilestoneQueryService milestoneQueryService;

    public MilestoneResource(
        MilestoneService milestoneService,
        MilestoneRepository milestoneRepository,
        MilestoneQueryService milestoneQueryService
    ) {
        this.milestoneService = milestoneService;
        this.milestoneRepository = milestoneRepository;
        this.milestoneQueryService = milestoneQueryService;
    }

    /**
     * {@code POST  /milestones} : Create a new milestone.
     *
     * @param milestoneDTO the milestoneDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new milestoneDTO, or with status {@code 400 (Bad Request)} if the milestone has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<MilestoneDTO> createMilestone(@Valid @RequestBody MilestoneDTO milestoneDTO) throws URISyntaxException {
        LOG.debug("REST request to save Milestone : {}", milestoneDTO);
        if (milestoneDTO.getId() != null) {
            throw new BadRequestAlertException("A new milestone cannot already have an ID", ENTITY_NAME, "idexists");
        }
        milestoneDTO = milestoneService.save(milestoneDTO);
        return ResponseEntity.created(new URI("/api/milestones/" + milestoneDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, milestoneDTO.getId().toString()))
            .body(milestoneDTO);
    }

    /**
     * {@code PUT  /milestones/:id} : Updates an existing milestone.
     *
     * @param id the id of the milestoneDTO to save.
     * @param milestoneDTO the milestoneDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated milestoneDTO,
     * or with status {@code 400 (Bad Request)} if the milestoneDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the milestoneDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<MilestoneDTO> updateMilestone(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody MilestoneDTO milestoneDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Milestone : {}, {}", id, milestoneDTO);
        if (milestoneDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, milestoneDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!milestoneRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        milestoneDTO = milestoneService.update(milestoneDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, milestoneDTO.getId().toString()))
            .body(milestoneDTO);
    }

    /**
     * {@code PATCH  /milestones/:id} : Partial updates given fields of an existing milestone, field will ignore if it is null
     *
     * @param id the id of the milestoneDTO to save.
     * @param milestoneDTO the milestoneDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated milestoneDTO,
     * or with status {@code 400 (Bad Request)} if the milestoneDTO is not valid,
     * or with status {@code 404 (Not Found)} if the milestoneDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the milestoneDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<MilestoneDTO> partialUpdateMilestone(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody MilestoneDTO milestoneDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Milestone partially : {}, {}", id, milestoneDTO);
        if (milestoneDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, milestoneDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!milestoneRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<MilestoneDTO> result = milestoneService.partialUpdate(milestoneDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, milestoneDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /milestones} : get all the milestones.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of milestones in body.
     */
    @GetMapping("")
    public ResponseEntity<List<MilestoneDTO>> getAllMilestones(
        MilestoneCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get Milestones by criteria: {}", criteria);

        Page<MilestoneDTO> page = milestoneQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /milestones/count} : count all the milestones.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countMilestones(MilestoneCriteria criteria) {
        LOG.debug("REST request to count Milestones by criteria: {}", criteria);
        return ResponseEntity.ok().body(milestoneQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /milestones/:id} : get the "id" milestone.
     *
     * @param id the id of the milestoneDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the milestoneDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<MilestoneDTO> getMilestone(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get Milestone : {}", id);
        Optional<MilestoneDTO> milestoneDTO = milestoneService.findOne(id);
        return ResponseUtil.wrapOrNotFound(milestoneDTO);
    }

    /**
     * {@code DELETE  /milestones/:id} : delete the "id" milestone.
     *
     * @param id the id of the milestoneDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMilestone(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete Milestone : {}", id);
        milestoneService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
