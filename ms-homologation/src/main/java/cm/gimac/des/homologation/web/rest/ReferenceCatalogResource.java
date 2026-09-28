package cm.gimac.des.homologation.web.rest;

import cm.gimac.des.homologation.repository.ReferenceCatalogRepository;
import cm.gimac.des.homologation.service.ReferenceCatalogService;
import cm.gimac.des.homologation.service.dto.ReferenceCatalogDTO;
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
 * REST controller for managing {@link cm.gimac.des.homologation.domain.ReferenceCatalog}.
 */
@RestController
@RequestMapping("/api/reference-catalogs")
public class ReferenceCatalogResource {

    private static final Logger LOG = LoggerFactory.getLogger(ReferenceCatalogResource.class);

    private static final String ENTITY_NAME = "homologationReferenceCatalog";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ReferenceCatalogService referenceCatalogService;

    private final ReferenceCatalogRepository referenceCatalogRepository;

    public ReferenceCatalogResource(
        ReferenceCatalogService referenceCatalogService,
        ReferenceCatalogRepository referenceCatalogRepository
    ) {
        this.referenceCatalogService = referenceCatalogService;
        this.referenceCatalogRepository = referenceCatalogRepository;
    }

    /**
     * {@code POST  /reference-catalogs} : Create a new referenceCatalog.
     *
     * @param referenceCatalogDTO the referenceCatalogDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new referenceCatalogDTO, or with status {@code 400 (Bad Request)} if the referenceCatalog has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<ReferenceCatalogDTO> createReferenceCatalog(@Valid @RequestBody ReferenceCatalogDTO referenceCatalogDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save ReferenceCatalog : {}", referenceCatalogDTO);
        if (referenceCatalogDTO.getId() != null) {
            throw new BadRequestAlertException("A new referenceCatalog cannot already have an ID", ENTITY_NAME, "idexists");
        }
        referenceCatalogDTO = referenceCatalogService.save(referenceCatalogDTO);
        return ResponseEntity.created(new URI("/api/reference-catalogs/" + referenceCatalogDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, referenceCatalogDTO.getId().toString()))
            .body(referenceCatalogDTO);
    }

    /**
     * {@code PUT  /reference-catalogs/:id} : Updates an existing referenceCatalog.
     *
     * @param id the id of the referenceCatalogDTO to save.
     * @param referenceCatalogDTO the referenceCatalogDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated referenceCatalogDTO,
     * or with status {@code 400 (Bad Request)} if the referenceCatalogDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the referenceCatalogDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ReferenceCatalogDTO> updateReferenceCatalog(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody ReferenceCatalogDTO referenceCatalogDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update ReferenceCatalog : {}, {}", id, referenceCatalogDTO);
        if (referenceCatalogDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, referenceCatalogDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!referenceCatalogRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        referenceCatalogDTO = referenceCatalogService.update(referenceCatalogDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, referenceCatalogDTO.getId().toString()))
            .body(referenceCatalogDTO);
    }

    /**
     * {@code PATCH  /reference-catalogs/:id} : Partial updates given fields of an existing referenceCatalog, field will ignore if it is null
     *
     * @param id the id of the referenceCatalogDTO to save.
     * @param referenceCatalogDTO the referenceCatalogDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated referenceCatalogDTO,
     * or with status {@code 400 (Bad Request)} if the referenceCatalogDTO is not valid,
     * or with status {@code 404 (Not Found)} if the referenceCatalogDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the referenceCatalogDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<ReferenceCatalogDTO> partialUpdateReferenceCatalog(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody ReferenceCatalogDTO referenceCatalogDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update ReferenceCatalog partially : {}, {}", id, referenceCatalogDTO);
        if (referenceCatalogDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, referenceCatalogDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!referenceCatalogRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<ReferenceCatalogDTO> result = referenceCatalogService.partialUpdate(referenceCatalogDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, referenceCatalogDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /reference-catalogs} : get all the referenceCatalogs.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of referenceCatalogs in body.
     */
    @GetMapping("")
    public ResponseEntity<List<ReferenceCatalogDTO>> getAllReferenceCatalogs(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get a page of ReferenceCatalogs");
        Page<ReferenceCatalogDTO> page = referenceCatalogService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /reference-catalogs/:id} : get the "id" referenceCatalog.
     *
     * @param id the id of the referenceCatalogDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the referenceCatalogDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReferenceCatalogDTO> getReferenceCatalog(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get ReferenceCatalog : {}", id);
        Optional<ReferenceCatalogDTO> referenceCatalogDTO = referenceCatalogService.findOne(id);
        return ResponseUtil.wrapOrNotFound(referenceCatalogDTO);
    }

    /**
     * {@code DELETE  /reference-catalogs/:id} : delete the "id" referenceCatalog.
     *
     * @param id the id of the referenceCatalogDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReferenceCatalog(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete ReferenceCatalog : {}", id);
        referenceCatalogService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
