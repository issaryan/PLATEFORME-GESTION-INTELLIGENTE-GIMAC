package cm.gimac.des.iam.web.rest;

import cm.gimac.des.iam.repository.AvailabilityRepository;
import cm.gimac.des.iam.service.AvailabilityQueryService;
import cm.gimac.des.iam.service.AvailabilityService;
import cm.gimac.des.iam.service.criteria.AvailabilityCriteria;
import cm.gimac.des.iam.service.dto.AvailabilityDTO;
import cm.gimac.des.iam.web.rest.errors.BadRequestAlertException;
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
 * REST controller for managing {@link cm.gimac.des.iam.domain.Availability}.
 */
@RestController
@RequestMapping("/api/availabilities")
public class AvailabilityResource {

    private static final Logger LOG = LoggerFactory.getLogger(AvailabilityResource.class);

    private static final String ENTITY_NAME = "iamAvailability";

    @Value("${jhipster.clientApp.name:iam}")
    private String applicationName;

    private final AvailabilityService availabilityService;

    private final AvailabilityRepository availabilityRepository;

    private final AvailabilityQueryService availabilityQueryService;

    public AvailabilityResource(
        AvailabilityService availabilityService,
        AvailabilityRepository availabilityRepository,
        AvailabilityQueryService availabilityQueryService
    ) {
        this.availabilityService = availabilityService;
        this.availabilityRepository = availabilityRepository;
        this.availabilityQueryService = availabilityQueryService;
    }

    /**
     * {@code POST  /availabilities} : Create a new availability.
     *
     * @param availabilityDTO the availabilityDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new availabilityDTO, or with status {@code 400 (Bad Request)} if the availability already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<AvailabilityDTO> createAvailability(@Valid @RequestBody AvailabilityDTO availabilityDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save Availability : {}", availabilityDTO);
        if (availabilityDTO.getId() != null) {
            throw new BadRequestAlertException("A new availability cannot already have an ID", ENTITY_NAME, "idexists");
        }
        availabilityDTO = availabilityService.save(availabilityDTO);
        return ResponseEntity.created(new URI("/api/availabilities/" + availabilityDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, availabilityDTO.getId().toString()))
            .body(availabilityDTO);
    }

    /**
     * {@code PUT  /availabilities/:id} : Updates an existing availability.
     *
     * @param id the id of the availabilityDTO to save.
     * @param availabilityDTO the availabilityDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated availabilityDTO,
     * or with status {@code 400 (Bad Request)} if the availabilityDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the availabilityDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<AvailabilityDTO> updateAvailability(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody AvailabilityDTO availabilityDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Availability : {}, {}", id, availabilityDTO);
        if (availabilityDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, availabilityDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!availabilityRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        availabilityDTO = availabilityService.update(availabilityDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, availabilityDTO.getId().toString()))
            .body(availabilityDTO);
    }

    /**
     * {@code PATCH  /availabilities/:id} : Partial updates given fields of an existing availability, field will ignore if it is null
     *
     * @param id the id of the availabilityDTO to save.
     * @param availabilityDTO the availabilityDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated availabilityDTO,
     * or with status {@code 400 (Bad Request)} if the availabilityDTO is not valid,
     * or with status {@code 404 (Not Found)} if the availabilityDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the availabilityDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<AvailabilityDTO> partialUpdateAvailability(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody AvailabilityDTO availabilityDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partially update Availability : {}, {}", id, availabilityDTO);
        if (availabilityDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, availabilityDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!availabilityRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<AvailabilityDTO> result = availabilityService.partialUpdate(availabilityDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, availabilityDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /availabilities} : get all the Availabilities.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Availabilities in body.
     */
    @GetMapping("")
    public ResponseEntity<List<AvailabilityDTO>> getAllAvailabilities(
        AvailabilityCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get Availabilities by criteria: {}", criteria);

        Page<AvailabilityDTO> page = availabilityQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /availabilities/count} : count all the availabilities.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countAvailabilities(AvailabilityCriteria criteria) {
        LOG.debug("REST request to count Availabilities by criteria: {}", criteria);
        return ResponseEntity.ok().body(availabilityQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /availabilities/:id} : get the "id" availability.
     *
     * @param id the id of the availabilityDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the availabilityDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AvailabilityDTO> getAvailability(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get Availability : {}", id);
        Optional<AvailabilityDTO> availabilityDTO = availabilityService.findOne(id);
        return ResponseUtil.wrapOrNotFound(availabilityDTO);
    }

    /**
     * {@code DELETE  /availabilities/:id} : delete the "id" availability.
     *
     * @param id the id of the availabilityDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAvailability(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete Availability : {}", id);
        availabilityService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
