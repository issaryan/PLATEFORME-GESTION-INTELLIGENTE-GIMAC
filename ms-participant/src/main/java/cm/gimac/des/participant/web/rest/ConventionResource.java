package cm.gimac.des.participant.web.rest;

import cm.gimac.des.participant.repository.ConventionRepository;
import cm.gimac.des.participant.service.ConventionQueryService;
import cm.gimac.des.participant.service.ConventionService;
import cm.gimac.des.participant.service.criteria.ConventionCriteria;
import cm.gimac.des.participant.service.dto.ConventionDTO;
import cm.gimac.des.participant.web.rest.errors.BadRequestAlertException;
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
 * REST controller for managing {@link cm.gimac.des.participant.domain.Convention}.
 */
@RestController
@RequestMapping("/api/conventions")
public class ConventionResource {

    private static final Logger LOG = LoggerFactory.getLogger(ConventionResource.class);

    private static final String ENTITY_NAME = "participantConvention";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ConventionService conventionService;

    private final ConventionRepository conventionRepository;

    private final ConventionQueryService conventionQueryService;

    public ConventionResource(
        ConventionService conventionService,
        ConventionRepository conventionRepository,
        ConventionQueryService conventionQueryService
    ) {
        this.conventionService = conventionService;
        this.conventionRepository = conventionRepository;
        this.conventionQueryService = conventionQueryService;
    }

    /**
     * {@code POST  /conventions} : Create a new convention.
     *
     * @param conventionDTO the conventionDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new conventionDTO, or with status {@code 400 (Bad Request)} if the convention has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<ConventionDTO> createConvention(@Valid @RequestBody ConventionDTO conventionDTO) throws URISyntaxException {
        LOG.debug("REST request to save Convention : {}", conventionDTO);
        if (conventionDTO.getId() != null) {
            throw new BadRequestAlertException("A new convention cannot already have an ID", ENTITY_NAME, "idexists");
        }
        conventionDTO = conventionService.save(conventionDTO);
        return ResponseEntity.created(new URI("/api/conventions/" + conventionDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, conventionDTO.getId().toString()))
            .body(conventionDTO);
    }

    /**
     * {@code PUT  /conventions/:id} : Updates an existing convention.
     *
     * @param id the id of the conventionDTO to save.
     * @param conventionDTO the conventionDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated conventionDTO,
     * or with status {@code 400 (Bad Request)} if the conventionDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the conventionDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ConventionDTO> updateConvention(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody ConventionDTO conventionDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Convention : {}, {}", id, conventionDTO);
        if (conventionDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, conventionDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!conventionRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        conventionDTO = conventionService.update(conventionDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, conventionDTO.getId().toString()))
            .body(conventionDTO);
    }

    /**
     * {@code PATCH  /conventions/:id} : Partial updates given fields of an existing convention, field will ignore if it is null
     *
     * @param id the id of the conventionDTO to save.
     * @param conventionDTO the conventionDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated conventionDTO,
     * or with status {@code 400 (Bad Request)} if the conventionDTO is not valid,
     * or with status {@code 404 (Not Found)} if the conventionDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the conventionDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<ConventionDTO> partialUpdateConvention(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody ConventionDTO conventionDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Convention partially : {}, {}", id, conventionDTO);
        if (conventionDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, conventionDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!conventionRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<ConventionDTO> result = conventionService.partialUpdate(conventionDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, conventionDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /conventions} : get all the conventions.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of conventions in body.
     */
    @GetMapping("")
    public ResponseEntity<List<ConventionDTO>> getAllConventions(
        ConventionCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get Conventions by criteria: {}", criteria);

        Page<ConventionDTO> page = conventionQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /conventions/count} : count all the conventions.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countConventions(ConventionCriteria criteria) {
        LOG.debug("REST request to count Conventions by criteria: {}", criteria);
        return ResponseEntity.ok().body(conventionQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /conventions/:id} : get the "id" convention.
     *
     * @param id the id of the conventionDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the conventionDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ConventionDTO> getConvention(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get Convention : {}", id);
        Optional<ConventionDTO> conventionDTO = conventionService.findOne(id);
        return ResponseUtil.wrapOrNotFound(conventionDTO);
    }

    /**
     * {@code DELETE  /conventions/:id} : delete the "id" convention.
     *
     * @param id the id of the conventionDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConvention(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete Convention : {}", id);
        conventionService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
