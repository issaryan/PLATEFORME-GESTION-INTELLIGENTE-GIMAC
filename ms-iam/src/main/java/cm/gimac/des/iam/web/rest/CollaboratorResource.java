package cm.gimac.des.iam.web.rest;

import cm.gimac.des.iam.repository.CollaboratorRepository;
import cm.gimac.des.iam.service.CollaboratorQueryService;
import cm.gimac.des.iam.service.CollaboratorService;
import cm.gimac.des.iam.service.criteria.CollaboratorCriteria;
import cm.gimac.des.iam.service.dto.CollaboratorDTO;
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
 * REST controller for managing {@link cm.gimac.des.iam.domain.Collaborator}.
 */
@RestController
@RequestMapping("/api/collaborators")
public class CollaboratorResource {

    private static final Logger LOG = LoggerFactory.getLogger(CollaboratorResource.class);

    private static final String ENTITY_NAME = "iamCollaborator";

    @Value("${jhipster.clientApp.name:iam}")
    private String applicationName;

    private final CollaboratorService collaboratorService;

    private final CollaboratorRepository collaboratorRepository;

    private final CollaboratorQueryService collaboratorQueryService;

    public CollaboratorResource(
        CollaboratorService collaboratorService,
        CollaboratorRepository collaboratorRepository,
        CollaboratorQueryService collaboratorQueryService
    ) {
        this.collaboratorService = collaboratorService;
        this.collaboratorRepository = collaboratorRepository;
        this.collaboratorQueryService = collaboratorQueryService;
    }

    /**
     * {@code POST  /collaborators} : Create a new collaborator.
     *
     * @param collaboratorDTO the collaboratorDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new collaboratorDTO, or with status {@code 400 (Bad Request)} if the collaborator already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<CollaboratorDTO> createCollaborator(@Valid @RequestBody CollaboratorDTO collaboratorDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save Collaborator : {}", collaboratorDTO);
        if (collaboratorDTO.getId() != null) {
            throw new BadRequestAlertException("A new collaborator cannot already have an ID", ENTITY_NAME, "idexists");
        }
        collaboratorDTO = collaboratorService.save(collaboratorDTO);
        return ResponseEntity.created(new URI("/api/collaborators/" + collaboratorDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, collaboratorDTO.getId().toString()))
            .body(collaboratorDTO);
    }

    /**
     * {@code PUT  /collaborators/:id} : Updates an existing collaborator.
     *
     * @param id the id of the collaboratorDTO to save.
     * @param collaboratorDTO the collaboratorDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated collaboratorDTO,
     * or with status {@code 400 (Bad Request)} if the collaboratorDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the collaboratorDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<CollaboratorDTO> updateCollaborator(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody CollaboratorDTO collaboratorDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Collaborator : {}, {}", id, collaboratorDTO);
        if (collaboratorDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, collaboratorDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!collaboratorRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        collaboratorDTO = collaboratorService.update(collaboratorDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, collaboratorDTO.getId().toString()))
            .body(collaboratorDTO);
    }

    /**
     * {@code PATCH  /collaborators/:id} : Partial updates given fields of an existing collaborator, field will ignore if it is null
     *
     * @param id the id of the collaboratorDTO to save.
     * @param collaboratorDTO the collaboratorDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated collaboratorDTO,
     * or with status {@code 400 (Bad Request)} if the collaboratorDTO is not valid,
     * or with status {@code 404 (Not Found)} if the collaboratorDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the collaboratorDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<CollaboratorDTO> partialUpdateCollaborator(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody CollaboratorDTO collaboratorDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partially update Collaborator : {}, {}", id, collaboratorDTO);
        if (collaboratorDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, collaboratorDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!collaboratorRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<CollaboratorDTO> result = collaboratorService.partialUpdate(collaboratorDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, collaboratorDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /collaborators} : get all the Collaborators.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Collaborators in body.
     */
    @GetMapping("")
    public ResponseEntity<List<CollaboratorDTO>> getAllCollaborators(
        CollaboratorCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get Collaborators by criteria: {}", criteria);

        Page<CollaboratorDTO> page = collaboratorQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /collaborators/count} : count all the collaborators.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countCollaborators(CollaboratorCriteria criteria) {
        LOG.debug("REST request to count Collaborators by criteria: {}", criteria);
        return ResponseEntity.ok().body(collaboratorQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /collaborators/:id} : get the "id" collaborator.
     *
     * @param id the id of the collaboratorDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the collaboratorDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CollaboratorDTO> getCollaborator(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get Collaborator : {}", id);
        Optional<CollaboratorDTO> collaboratorDTO = collaboratorService.findOne(id);
        return ResponseUtil.wrapOrNotFound(collaboratorDTO);
    }

    /**
     * {@code DELETE  /collaborators/:id} : delete the "id" collaborator.
     *
     * @param id the id of the collaboratorDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCollaborator(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete Collaborator : {}", id);
        collaboratorService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
