package cm.gimac.des.iam.web.rest;

import cm.gimac.des.iam.repository.RoleGrantRepository;
import cm.gimac.des.iam.service.RoleGrantQueryService;
import cm.gimac.des.iam.service.RoleGrantService;
import cm.gimac.des.iam.service.criteria.RoleGrantCriteria;
import cm.gimac.des.iam.service.dto.RoleGrantDTO;
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
 * REST controller for managing {@link cm.gimac.des.iam.domain.RoleGrant}.
 */
@RestController
@RequestMapping("/api/role-grants")
public class RoleGrantResource {

    private static final Logger LOG = LoggerFactory.getLogger(RoleGrantResource.class);

    private static final String ENTITY_NAME = "iamRoleGrant";

    @Value("${jhipster.clientApp.name:iam}")
    private String applicationName;

    private final RoleGrantService roleGrantService;

    private final RoleGrantRepository roleGrantRepository;

    private final RoleGrantQueryService roleGrantQueryService;

    public RoleGrantResource(
        RoleGrantService roleGrantService,
        RoleGrantRepository roleGrantRepository,
        RoleGrantQueryService roleGrantQueryService
    ) {
        this.roleGrantService = roleGrantService;
        this.roleGrantRepository = roleGrantRepository;
        this.roleGrantQueryService = roleGrantQueryService;
    }

    /**
     * {@code POST  /role-grants} : Create a new roleGrant.
     *
     * @param roleGrantDTO the roleGrantDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new roleGrantDTO, or with status {@code 400 (Bad Request)} if the roleGrant already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<RoleGrantDTO> createRoleGrant(@Valid @RequestBody RoleGrantDTO roleGrantDTO) throws URISyntaxException {
        LOG.debug("REST request to save RoleGrant : {}", roleGrantDTO);
        if (roleGrantDTO.getId() != null) {
            throw new BadRequestAlertException("A new roleGrant cannot already have an ID", ENTITY_NAME, "idexists");
        }
        roleGrantDTO = roleGrantService.save(roleGrantDTO);
        return ResponseEntity.created(new URI("/api/role-grants/" + roleGrantDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, roleGrantDTO.getId().toString()))
            .body(roleGrantDTO);
    }

    /**
     * {@code PUT  /role-grants/:id} : Updates an existing roleGrant.
     *
     * @param id the id of the roleGrantDTO to save.
     * @param roleGrantDTO the roleGrantDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated roleGrantDTO,
     * or with status {@code 400 (Bad Request)} if the roleGrantDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the roleGrantDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<RoleGrantDTO> updateRoleGrant(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody RoleGrantDTO roleGrantDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update RoleGrant : {}, {}", id, roleGrantDTO);
        if (roleGrantDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, roleGrantDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!roleGrantRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        roleGrantDTO = roleGrantService.update(roleGrantDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, roleGrantDTO.getId().toString()))
            .body(roleGrantDTO);
    }

    /**
     * {@code PATCH  /role-grants/:id} : Partial updates given fields of an existing roleGrant, field will ignore if it is null
     *
     * @param id the id of the roleGrantDTO to save.
     * @param roleGrantDTO the roleGrantDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated roleGrantDTO,
     * or with status {@code 400 (Bad Request)} if the roleGrantDTO is not valid,
     * or with status {@code 404 (Not Found)} if the roleGrantDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the roleGrantDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<RoleGrantDTO> partialUpdateRoleGrant(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody RoleGrantDTO roleGrantDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partially update RoleGrant : {}, {}", id, roleGrantDTO);
        if (roleGrantDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, roleGrantDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!roleGrantRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<RoleGrantDTO> result = roleGrantService.partialUpdate(roleGrantDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, roleGrantDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /role-grants} : get all the Role Grants.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Role Grants in body.
     */
    @GetMapping("")
    public ResponseEntity<List<RoleGrantDTO>> getAllRoleGrants(
        RoleGrantCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get RoleGrants by criteria: {}", criteria);

        Page<RoleGrantDTO> page = roleGrantQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /role-grants/count} : count all the roleGrants.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countRoleGrants(RoleGrantCriteria criteria) {
        LOG.debug("REST request to count RoleGrants by criteria: {}", criteria);
        return ResponseEntity.ok().body(roleGrantQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /role-grants/:id} : get the "id" roleGrant.
     *
     * @param id the id of the roleGrantDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the roleGrantDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RoleGrantDTO> getRoleGrant(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get RoleGrant : {}", id);
        Optional<RoleGrantDTO> roleGrantDTO = roleGrantService.findOne(id);
        return ResponseUtil.wrapOrNotFound(roleGrantDTO);
    }

    /**
     * {@code DELETE  /role-grants/:id} : delete the "id" roleGrant.
     *
     * @param id the id of the roleGrantDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoleGrant(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete RoleGrant : {}", id);
        roleGrantService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
