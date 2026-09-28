package cm.gimac.des.pilotage.web.rest;

import cm.gimac.des.pilotage.repository.MeetingAttendanceRepository;
import cm.gimac.des.pilotage.service.MeetingAttendanceService;
import cm.gimac.des.pilotage.service.dto.MeetingAttendanceDTO;
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
 * REST controller for managing {@link cm.gimac.des.pilotage.domain.MeetingAttendance}.
 */
@RestController
@RequestMapping("/api/meeting-attendances")
public class MeetingAttendanceResource {

    private static final Logger LOG = LoggerFactory.getLogger(MeetingAttendanceResource.class);

    private static final String ENTITY_NAME = "pilotageMeetingAttendance";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final MeetingAttendanceService meetingAttendanceService;

    private final MeetingAttendanceRepository meetingAttendanceRepository;

    public MeetingAttendanceResource(
        MeetingAttendanceService meetingAttendanceService,
        MeetingAttendanceRepository meetingAttendanceRepository
    ) {
        this.meetingAttendanceService = meetingAttendanceService;
        this.meetingAttendanceRepository = meetingAttendanceRepository;
    }

    /**
     * {@code POST  /meeting-attendances} : Create a new meetingAttendance.
     *
     * @param meetingAttendanceDTO the meetingAttendanceDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new meetingAttendanceDTO, or with status {@code 400 (Bad Request)} if the meetingAttendance has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<MeetingAttendanceDTO> createMeetingAttendance(@Valid @RequestBody MeetingAttendanceDTO meetingAttendanceDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save MeetingAttendance : {}", meetingAttendanceDTO);
        if (meetingAttendanceDTO.getId() != null) {
            throw new BadRequestAlertException("A new meetingAttendance cannot already have an ID", ENTITY_NAME, "idexists");
        }
        meetingAttendanceDTO = meetingAttendanceService.save(meetingAttendanceDTO);
        return ResponseEntity.created(new URI("/api/meeting-attendances/" + meetingAttendanceDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, meetingAttendanceDTO.getId().toString()))
            .body(meetingAttendanceDTO);
    }

    /**
     * {@code PUT  /meeting-attendances/:id} : Updates an existing meetingAttendance.
     *
     * @param id the id of the meetingAttendanceDTO to save.
     * @param meetingAttendanceDTO the meetingAttendanceDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated meetingAttendanceDTO,
     * or with status {@code 400 (Bad Request)} if the meetingAttendanceDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the meetingAttendanceDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<MeetingAttendanceDTO> updateMeetingAttendance(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody MeetingAttendanceDTO meetingAttendanceDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update MeetingAttendance : {}, {}", id, meetingAttendanceDTO);
        if (meetingAttendanceDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, meetingAttendanceDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!meetingAttendanceRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        meetingAttendanceDTO = meetingAttendanceService.update(meetingAttendanceDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, meetingAttendanceDTO.getId().toString()))
            .body(meetingAttendanceDTO);
    }

    /**
     * {@code PATCH  /meeting-attendances/:id} : Partial updates given fields of an existing meetingAttendance, field will ignore if it is null
     *
     * @param id the id of the meetingAttendanceDTO to save.
     * @param meetingAttendanceDTO the meetingAttendanceDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated meetingAttendanceDTO,
     * or with status {@code 400 (Bad Request)} if the meetingAttendanceDTO is not valid,
     * or with status {@code 404 (Not Found)} if the meetingAttendanceDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the meetingAttendanceDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<MeetingAttendanceDTO> partialUpdateMeetingAttendance(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody MeetingAttendanceDTO meetingAttendanceDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update MeetingAttendance partially : {}, {}", id, meetingAttendanceDTO);
        if (meetingAttendanceDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, meetingAttendanceDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!meetingAttendanceRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<MeetingAttendanceDTO> result = meetingAttendanceService.partialUpdate(meetingAttendanceDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, meetingAttendanceDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /meeting-attendances} : get all the meetingAttendances.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of meetingAttendances in body.
     */
    @GetMapping("")
    public List<MeetingAttendanceDTO> getAllMeetingAttendances(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all MeetingAttendances");
        return meetingAttendanceService.findAll();
    }

    /**
     * {@code GET  /meeting-attendances/:id} : get the "id" meetingAttendance.
     *
     * @param id the id of the meetingAttendanceDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the meetingAttendanceDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<MeetingAttendanceDTO> getMeetingAttendance(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get MeetingAttendance : {}", id);
        Optional<MeetingAttendanceDTO> meetingAttendanceDTO = meetingAttendanceService.findOne(id);
        return ResponseUtil.wrapOrNotFound(meetingAttendanceDTO);
    }

    /**
     * {@code DELETE  /meeting-attendances/:id} : delete the "id" meetingAttendance.
     *
     * @param id the id of the meetingAttendanceDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMeetingAttendance(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete MeetingAttendance : {}", id);
        meetingAttendanceService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
