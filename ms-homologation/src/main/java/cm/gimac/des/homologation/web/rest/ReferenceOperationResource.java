package cm.gimac.des.homologation.web.rest;

import cm.gimac.des.homologation.repository.ReferenceOperationRepository;
import cm.gimac.des.homologation.service.ReferenceOperationQueryService;
import cm.gimac.des.homologation.service.ReferenceOperationService;
import cm.gimac.des.homologation.service.criteria.ReferenceOperationCriteria;
import cm.gimac.des.homologation.service.dto.ReferenceOperationDTO;
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
 * REST controller for managing {@link cm.gimac.des.homologation.domain.ReferenceOperation}.
 */
@RestController
@RequestMapping("/api/reference-operations")
public class ReferenceOperationResource {

    private static final Logger LOG = LoggerFactory.getLogger(ReferenceOperationResource.class);

    private static final String ENTITY_NAME = "homologationReferenceOperation";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ReferenceOperationService referenceOperationService;

    private final ReferenceOperationRepository referenceOperationRepository;

    private final ReferenceOperationQueryService referenceOperationQueryService;

    public ReferenceOperationResource(
        ReferenceOperationService referenceOperationService,
        ReferenceOperationRepository referenceOperationRepository,
        ReferenceOperationQueryService referenceOperationQueryService
    ) {
        this.referenceOperationService = referenceOperationService;
        this.referenceOperationRepository = referenceOperationRepository;
        this.referenceOperationQueryService = referenceOperationQueryService;
    }

    /**
     * {@code POST  /reference-operations} : Create a new referenceOperation.
     *
     * @param referenceOperationDTO the referenceOperationDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new referenceOperationDTO, or with status {@code 400 (Bad Request)} if the referenceOperation has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<ReferenceOperationDTO> createReferenceOperation(@Valid @RequestBody ReferenceOperationDTO referenceOperationDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save ReferenceOperation : {}", referenceOperationDTO);
        if (referenceOperationDTO.getId() != null) {
            throw new BadRequestAlertException("A new referenceOperation cannot already have an ID", ENTITY_NAME, "idexists");
        }
        referenceOperationDTO = referenceOperationService.save(referenceOperationDTO);
        return ResponseEntity.created(new URI("/api/reference-operations/" + referenceOperationDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, referenceOperationDTO.getId().toString()))
            .body(referenceOperationDTO);
    }

    /**
     * {@code PUT  /reference-operations/:id} : Updates an existing referenceOperation.
     *
     * @param id the id of the referenceOperationDTO to save.
     * @param referenceOperationDTO the referenceOperationDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated referenceOperationDTO,
     * or with status {@code 400 (Bad Request)} if the referenceOperationDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the referenceOperationDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ReferenceOperationDTO> updateReferenceOperation(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody ReferenceOperationDTO referenceOperationDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update ReferenceOperation : {}, {}", id, referenceOperationDTO);
        if (referenceOperationDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, referenceOperationDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!referenceOperationRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        referenceOperationDTO = referenceOperationService.update(referenceOperationDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, referenceOperationDTO.getId().toString()))
            .body(referenceOperationDTO);
    }

    /**
     * {@code PATCH  /reference-operations/:id} : Partial updates given fields of an existing referenceOperation, field will ignore if it is null
     *
     * @param id the id of the referenceOperationDTO to save.
     * @param referenceOperationDTO the referenceOperationDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated referenceOperationDTO,
     * or with status {@code 400 (Bad Request)} if the referenceOperationDTO is not valid,
     * or with status {@code 404 (Not Found)} if the referenceOperationDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the referenceOperationDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<ReferenceOperationDTO> partialUpdateReferenceOperation(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody ReferenceOperationDTO referenceOperationDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update ReferenceOperation partially : {}, {}", id, referenceOperationDTO);
        if (referenceOperationDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, referenceOperationDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!referenceOperationRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<ReferenceOperationDTO> result = referenceOperationService.partialUpdate(referenceOperationDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, referenceOperationDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /reference-operations} : get all the referenceOperations.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of referenceOperations in body.
     */
    @GetMapping("")
    public ResponseEntity<List<ReferenceOperationDTO>> getAllReferenceOperations(
        ReferenceOperationCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get ReferenceOperations by criteria: {}", criteria);

        Page<ReferenceOperationDTO> page = referenceOperationQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /reference-operations/count} : count all the referenceOperations.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countReferenceOperations(ReferenceOperationCriteria criteria) {
        LOG.debug("REST request to count ReferenceOperations by criteria: {}", criteria);
        return ResponseEntity.ok().body(referenceOperationQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /reference-operations/:id} : get the "id" referenceOperation.
     *
     * @param id the id of the referenceOperationDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the referenceOperationDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReferenceOperationDTO> getReferenceOperation(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get ReferenceOperation : {}", id);
        Optional<ReferenceOperationDTO> referenceOperationDTO = referenceOperationService.findOne(id);
        return ResponseUtil.wrapOrNotFound(referenceOperationDTO);
    }

    /**
     * {@code DELETE  /reference-operations/:id} : delete the "id" referenceOperation.
     *
     * @param id the id of the referenceOperationDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReferenceOperation(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete ReferenceOperation : {}", id);
        referenceOperationService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
