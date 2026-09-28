package cm.gimac.des.pilotage.web.rest;

import cm.gimac.des.pilotage.repository.MeetingRepository;
import cm.gimac.des.pilotage.service.MeetingQueryService;
import cm.gimac.des.pilotage.service.MeetingService;
import cm.gimac.des.pilotage.service.criteria.MeetingCriteria;
import cm.gimac.des.pilotage.service.dto.MeetingDTO;
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
 * REST controller for managing {@link cm.gimac.des.pilotage.domain.Meeting}.
 */
@RestController
@RequestMapping("/api/meetings")
public class MeetingResource {

    private static final Logger LOG = LoggerFactory.getLogger(MeetingResource.class);

    private static final String ENTITY_NAME = "pilotageMeeting";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final MeetingService meetingService;

    private final MeetingRepository meetingRepository;

    private final MeetingQueryService meetingQueryService;

    public MeetingResource(MeetingService meetingService, MeetingRepository meetingRepository, MeetingQueryService meetingQueryService) {
        this.meetingService = meetingService;
        this.meetingRepository = meetingRepository;
        this.meetingQueryService = meetingQueryService;
    }

    /**
     * {@code POST  /meetings} : Create a new meeting.
     *
     * @param meetingDTO the meetingDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new meetingDTO, or with status {@code 400 (Bad Request)} if the meeting has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<MeetingDTO> createMeeting(@Valid @RequestBody MeetingDTO meetingDTO) throws URISyntaxException {
        LOG.debug("REST request to save Meeting : {}", meetingDTO);
        if (meetingDTO.getId() != null) {
            throw new BadRequestAlertException("A new meeting cannot already have an ID", ENTITY_NAME, "idexists");
        }
        meetingDTO = meetingService.save(meetingDTO);
        return ResponseEntity.created(new URI("/api/meetings/" + meetingDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, meetingDTO.getId().toString()))
            .body(meetingDTO);
    }

    /**
     * {@code PUT  /meetings/:id} : Updates an existing meeting.
     *
     * @param id the id of the meetingDTO to save.
     * @param meetingDTO the meetingDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated meetingDTO,
     * or with status {@code 400 (Bad Request)} if the meetingDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the meetingDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<MeetingDTO> updateMeeting(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody MeetingDTO meetingDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Meeting : {}, {}", id, meetingDTO);
        if (meetingDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, meetingDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!meetingRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        meetingDTO = meetingService.update(meetingDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, meetingDTO.getId().toString()))
            .body(meetingDTO);
    }

    /**
     * {@code PATCH  /meetings/:id} : Partial updates given fields of an existing meeting, field will ignore if it is null
     *
     * @param id the id of the meetingDTO to save.
     * @param meetingDTO the meetingDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated meetingDTO,
     * or with status {@code 400 (Bad Request)} if the meetingDTO is not valid,
     * or with status {@code 404 (Not Found)} if the meetingDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the meetingDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<MeetingDTO> partialUpdateMeeting(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody MeetingDTO meetingDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Meeting partially : {}, {}", id, meetingDTO);
        if (meetingDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, meetingDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!meetingRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<MeetingDTO> result = meetingService.partialUpdate(meetingDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, meetingDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /meetings} : get all the meetings.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of meetings in body.
     */
    @GetMapping("")
    public ResponseEntity<List<MeetingDTO>> getAllMeetings(
        MeetingCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get Meetings by criteria: {}", criteria);

        Page<MeetingDTO> page = meetingQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /meetings/count} : count all the meetings.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countMeetings(MeetingCriteria criteria) {
        LOG.debug("REST request to count Meetings by criteria: {}", criteria);
        return ResponseEntity.ok().body(meetingQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /meetings/:id} : get the "id" meeting.
     *
     * @param id the id of the meetingDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the meetingDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<MeetingDTO> getMeeting(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get Meeting : {}", id);
        Optional<MeetingDTO> meetingDTO = meetingService.findOne(id);
        return ResponseUtil.wrapOrNotFound(meetingDTO);
    }

    /**
     * {@code DELETE  /meetings/:id} : delete the "id" meeting.
     *
     * @param id the id of the meetingDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMeeting(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete Meeting : {}", id);
        meetingService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
