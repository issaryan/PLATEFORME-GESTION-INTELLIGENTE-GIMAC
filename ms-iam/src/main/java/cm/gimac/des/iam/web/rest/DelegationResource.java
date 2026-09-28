package cm.gimac.des.iam.web.rest;

import cm.gimac.des.iam.repository.DelegationRepository;
import cm.gimac.des.iam.service.DelegationQueryService;
import cm.gimac.des.iam.service.DelegationService;
import cm.gimac.des.iam.service.criteria.DelegationCriteria;
import cm.gimac.des.iam.service.dto.DelegationDTO;
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
 * REST controller for managing {@link cm.gimac.des.iam.domain.Delegation}.
 */
@RestController
@RequestMapping("/api/delegations")
public class DelegationResource {

    private static final Logger LOG = LoggerFactory.getLogger(DelegationResource.class);

    private static final String ENTITY_NAME = "iamDelegation";

    @Value("${jhipster.clientApp.name:iam}")
    private String applicationName;

    private final DelegationService delegationService;

    private final DelegationRepository delegationRepository;

    private final DelegationQueryService delegationQueryService;

    public DelegationResource(
        DelegationService delegationService,
        DelegationRepository delegationRepository,
        DelegationQueryService delegationQueryService
    ) {
        this.delegationService = delegationService;
        this.delegationRepository = delegationRepository;
        this.delegationQueryService = delegationQueryService;
    }

    /**
     * {@code POST  /delegations} : Create a new delegation.
     *
     * @param delegationDTO the delegationDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new delegationDTO, or with status {@code 400 (Bad Request)} if the delegation already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<DelegationDTO> createDelegation(@Valid @RequestBody DelegationDTO delegationDTO) throws URISyntaxException {
        LOG.debug("REST request to save Delegation : {}", delegationDTO);
        if (delegationDTO.getId() != null) {
            throw new BadRequestAlertException("A new delegation cannot already have an ID", ENTITY_NAME, "idexists");
        }
        delegationDTO = delegationService.save(delegationDTO);
        return ResponseEntity.created(new URI("/api/delegations/" + delegationDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, delegationDTO.getId().toString()))
            .body(delegationDTO);
    }

    /**
     * {@code PUT  /delegations/:id} : Updates an existing delegation.
     *
     * @param id the id of the delegationDTO to save.
     * @param delegationDTO the delegationDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated delegationDTO,
     * or with status {@code 400 (Bad Request)} if the delegationDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the delegationDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<DelegationDTO> updateDelegation(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody DelegationDTO delegationDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Delegation : {}, {}", id, delegationDTO);
        if (delegationDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, delegationDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!delegationRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        delegationDTO = delegationService.update(delegationDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, delegationDTO.getId().toString()))
            .body(delegationDTO);
    }

    /**
     * {@code PATCH  /delegations/:id} : Partial updates given fields of an existing delegation, field will ignore if it is null
     *
     * @param id the id of the delegationDTO to save.
     * @param delegationDTO the delegationDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated delegationDTO,
     * or with status {@code 400 (Bad Request)} if the delegationDTO is not valid,
     * or with status {@code 404 (Not Found)} if the delegationDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the delegationDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<DelegationDTO> partialUpdateDelegation(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody DelegationDTO delegationDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partially update Delegation : {}, {}", id, delegationDTO);
        if (delegationDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, delegationDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!delegationRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<DelegationDTO> result = delegationService.partialUpdate(delegationDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, delegationDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /delegations} : get all the Delegations.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Delegations in body.
     */
    @GetMapping("")
    public ResponseEntity<List<DelegationDTO>> getAllDelegations(
        DelegationCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get Delegations by criteria: {}", criteria);

        Page<DelegationDTO> page = delegationQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /delegations/count} : count all the delegations.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countDelegations(DelegationCriteria criteria) {
        LOG.debug("REST request to count Delegations by criteria: {}", criteria);
        return ResponseEntity.ok().body(delegationQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /delegations/:id} : get the "id" delegation.
     *
     * @param id the id of the delegationDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the delegationDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DelegationDTO> getDelegation(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get Delegation : {}", id);
        Optional<DelegationDTO> delegationDTO = delegationService.findOne(id);
        return ResponseUtil.wrapOrNotFound(delegationDTO);
    }

    /**
     * {@code DELETE  /delegations/:id} : delete the "id" delegation.
     *
     * @param id the id of the delegationDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDelegation(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete Delegation : {}", id);
        delegationService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
