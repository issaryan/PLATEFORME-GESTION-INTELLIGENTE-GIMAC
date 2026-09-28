package cm.gimac.des.collaboration.web.rest;

import cm.gimac.des.collaboration.repository.ConferenceRepository;
import cm.gimac.des.collaboration.service.ConferenceQueryService;
import cm.gimac.des.collaboration.service.ConferenceService;
import cm.gimac.des.collaboration.service.criteria.ConferenceCriteria;
import cm.gimac.des.collaboration.service.dto.ConferenceDTO;
import cm.gimac.des.collaboration.web.rest.errors.BadRequestAlertException;
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
 * REST controller for managing {@link cm.gimac.des.collaboration.domain.Conference}.
 */
@RestController
@RequestMapping("/api/conferences")
public class ConferenceResource {

    private static final Logger LOG = LoggerFactory.getLogger(ConferenceResource.class);

    private static final String ENTITY_NAME = "collaborationConference";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ConferenceService conferenceService;

    private final ConferenceRepository conferenceRepository;

    private final ConferenceQueryService conferenceQueryService;

    public ConferenceResource(
        ConferenceService conferenceService,
        ConferenceRepository conferenceRepository,
        ConferenceQueryService conferenceQueryService
    ) {
        this.conferenceService = conferenceService;
        this.conferenceRepository = conferenceRepository;
        this.conferenceQueryService = conferenceQueryService;
    }

    /**
     * {@code POST  /conferences} : Create a new conference.
     *
     * @param conferenceDTO the conferenceDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new conferenceDTO, or with status {@code 400 (Bad Request)} if the conference has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<ConferenceDTO> createConference(@Valid @RequestBody ConferenceDTO conferenceDTO) throws URISyntaxException {
        LOG.debug("REST request to save Conference : {}", conferenceDTO);
        if (conferenceDTO.getId() != null) {
            throw new BadRequestAlertException("A new conference cannot already have an ID", ENTITY_NAME, "idexists");
        }
        conferenceDTO = conferenceService.save(conferenceDTO);
        return ResponseEntity.created(new URI("/api/conferences/" + conferenceDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, conferenceDTO.getId().toString()))
            .body(conferenceDTO);
    }

    /**
     * {@code PUT  /conferences/:id} : Updates an existing conference.
     *
     * @param id the id of the conferenceDTO to save.
     * @param conferenceDTO the conferenceDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated conferenceDTO,
     * or with status {@code 400 (Bad Request)} if the conferenceDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the conferenceDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ConferenceDTO> updateConference(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody ConferenceDTO conferenceDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Conference : {}, {}", id, conferenceDTO);
        if (conferenceDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, conferenceDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!conferenceRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        conferenceDTO = conferenceService.update(conferenceDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, conferenceDTO.getId().toString()))
            .body(conferenceDTO);
    }

    /**
     * {@code PATCH  /conferences/:id} : Partial updates given fields of an existing conference, field will ignore if it is null
     *
     * @param id the id of the conferenceDTO to save.
     * @param conferenceDTO the conferenceDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated conferenceDTO,
     * or with status {@code 400 (Bad Request)} if the conferenceDTO is not valid,
     * or with status {@code 404 (Not Found)} if the conferenceDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the conferenceDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<ConferenceDTO> partialUpdateConference(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody ConferenceDTO conferenceDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Conference partially : {}, {}", id, conferenceDTO);
        if (conferenceDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, conferenceDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!conferenceRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<ConferenceDTO> result = conferenceService.partialUpdate(conferenceDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, conferenceDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /conferences} : get all the conferences.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of conferences in body.
     */
    @GetMapping("")
    public ResponseEntity<List<ConferenceDTO>> getAllConferences(
        ConferenceCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get Conferences by criteria: {}", criteria);

        Page<ConferenceDTO> page = conferenceQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /conferences/count} : count all the conferences.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countConferences(ConferenceCriteria criteria) {
        LOG.debug("REST request to count Conferences by criteria: {}", criteria);
        return ResponseEntity.ok().body(conferenceQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /conferences/:id} : get the "id" conference.
     *
     * @param id the id of the conferenceDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the conferenceDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ConferenceDTO> getConference(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get Conference : {}", id);
        Optional<ConferenceDTO> conferenceDTO = conferenceService.findOne(id);
        return ResponseUtil.wrapOrNotFound(conferenceDTO);
    }

    /**
     * {@code DELETE  /conferences/:id} : delete the "id" conference.
     *
     * @param id the id of the conferenceDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConference(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete Conference : {}", id);
        conferenceService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
