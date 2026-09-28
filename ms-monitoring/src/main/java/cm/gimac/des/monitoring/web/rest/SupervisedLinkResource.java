package cm.gimac.des.monitoring.web.rest;

import cm.gimac.des.monitoring.repository.SupervisedLinkRepository;
import cm.gimac.des.monitoring.service.SupervisedLinkQueryService;
import cm.gimac.des.monitoring.service.SupervisedLinkService;
import cm.gimac.des.monitoring.service.criteria.SupervisedLinkCriteria;
import cm.gimac.des.monitoring.service.dto.SupervisedLinkDTO;
import cm.gimac.des.monitoring.web.rest.errors.BadRequestAlertException;
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
 * REST controller for managing {@link cm.gimac.des.monitoring.domain.SupervisedLink}.
 */
@RestController
@RequestMapping("/api/supervised-links")
public class SupervisedLinkResource {

    private static final Logger LOG = LoggerFactory.getLogger(SupervisedLinkResource.class);

    private static final String ENTITY_NAME = "monitoringSupervisedLink";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final SupervisedLinkService supervisedLinkService;

    private final SupervisedLinkRepository supervisedLinkRepository;

    private final SupervisedLinkQueryService supervisedLinkQueryService;

    public SupervisedLinkResource(
        SupervisedLinkService supervisedLinkService,
        SupervisedLinkRepository supervisedLinkRepository,
        SupervisedLinkQueryService supervisedLinkQueryService
    ) {
        this.supervisedLinkService = supervisedLinkService;
        this.supervisedLinkRepository = supervisedLinkRepository;
        this.supervisedLinkQueryService = supervisedLinkQueryService;
    }

    /**
     * {@code POST  /supervised-links} : Create a new supervisedLink.
     *
     * @param supervisedLinkDTO the supervisedLinkDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new supervisedLinkDTO, or with status {@code 400 (Bad Request)} if the supervisedLink has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<SupervisedLinkDTO> createSupervisedLink(@Valid @RequestBody SupervisedLinkDTO supervisedLinkDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save SupervisedLink : {}", supervisedLinkDTO);
        if (supervisedLinkDTO.getId() != null) {
            throw new BadRequestAlertException("A new supervisedLink cannot already have an ID", ENTITY_NAME, "idexists");
        }
        supervisedLinkDTO = supervisedLinkService.save(supervisedLinkDTO);
        return ResponseEntity.created(new URI("/api/supervised-links/" + supervisedLinkDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, supervisedLinkDTO.getId().toString()))
            .body(supervisedLinkDTO);
    }

    /**
     * {@code PUT  /supervised-links/:id} : Updates an existing supervisedLink.
     *
     * @param id the id of the supervisedLinkDTO to save.
     * @param supervisedLinkDTO the supervisedLinkDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated supervisedLinkDTO,
     * or with status {@code 400 (Bad Request)} if the supervisedLinkDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the supervisedLinkDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<SupervisedLinkDTO> updateSupervisedLink(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody SupervisedLinkDTO supervisedLinkDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update SupervisedLink : {}, {}", id, supervisedLinkDTO);
        if (supervisedLinkDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, supervisedLinkDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!supervisedLinkRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        supervisedLinkDTO = supervisedLinkService.update(supervisedLinkDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, supervisedLinkDTO.getId().toString()))
            .body(supervisedLinkDTO);
    }

    /**
     * {@code PATCH  /supervised-links/:id} : Partial updates given fields of an existing supervisedLink, field will ignore if it is null
     *
     * @param id the id of the supervisedLinkDTO to save.
     * @param supervisedLinkDTO the supervisedLinkDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated supervisedLinkDTO,
     * or with status {@code 400 (Bad Request)} if the supervisedLinkDTO is not valid,
     * or with status {@code 404 (Not Found)} if the supervisedLinkDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the supervisedLinkDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<SupervisedLinkDTO> partialUpdateSupervisedLink(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody SupervisedLinkDTO supervisedLinkDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update SupervisedLink partially : {}, {}", id, supervisedLinkDTO);
        if (supervisedLinkDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, supervisedLinkDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!supervisedLinkRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<SupervisedLinkDTO> result = supervisedLinkService.partialUpdate(supervisedLinkDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, supervisedLinkDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /supervised-links} : get all the supervisedLinks.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of supervisedLinks in body.
     */
    @GetMapping("")
    public ResponseEntity<List<SupervisedLinkDTO>> getAllSupervisedLinks(
        SupervisedLinkCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get SupervisedLinks by criteria: {}", criteria);

        Page<SupervisedLinkDTO> page = supervisedLinkQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /supervised-links/count} : count all the supervisedLinks.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countSupervisedLinks(SupervisedLinkCriteria criteria) {
        LOG.debug("REST request to count SupervisedLinks by criteria: {}", criteria);
        return ResponseEntity.ok().body(supervisedLinkQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /supervised-links/:id} : get the "id" supervisedLink.
     *
     * @param id the id of the supervisedLinkDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the supervisedLinkDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SupervisedLinkDTO> getSupervisedLink(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get SupervisedLink : {}", id);
        Optional<SupervisedLinkDTO> supervisedLinkDTO = supervisedLinkService.findOne(id);
        return ResponseUtil.wrapOrNotFound(supervisedLinkDTO);
    }

    /**
     * {@code DELETE  /supervised-links/:id} : delete the "id" supervisedLink.
     *
     * @param id the id of the supervisedLinkDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSupervisedLink(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete SupervisedLink : {}", id);
        supervisedLinkService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
