package cm.gimac.des.document.web.rest;

import cm.gimac.des.document.repository.DocumentTypePolicyRepository;
import cm.gimac.des.document.service.DocumentTypePolicyQueryService;
import cm.gimac.des.document.service.DocumentTypePolicyService;
import cm.gimac.des.document.service.criteria.DocumentTypePolicyCriteria;
import cm.gimac.des.document.service.dto.DocumentTypePolicyDTO;
import cm.gimac.des.document.web.rest.errors.BadRequestAlertException;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link cm.gimac.des.document.domain.DocumentTypePolicy}.
 */
@RestController
@RequestMapping("/api/document-type-policies")
public class DocumentTypePolicyResource {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentTypePolicyResource.class);

    private static final String ENTITY_NAME = "documentDocumentTypePolicy";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final DocumentTypePolicyService documentTypePolicyService;

    private final DocumentTypePolicyRepository documentTypePolicyRepository;

    private final DocumentTypePolicyQueryService documentTypePolicyQueryService;

    public DocumentTypePolicyResource(
        DocumentTypePolicyService documentTypePolicyService,
        DocumentTypePolicyRepository documentTypePolicyRepository,
        DocumentTypePolicyQueryService documentTypePolicyQueryService
    ) {
        this.documentTypePolicyService = documentTypePolicyService;
        this.documentTypePolicyRepository = documentTypePolicyRepository;
        this.documentTypePolicyQueryService = documentTypePolicyQueryService;
    }

    /**
     * {@code POST  /document-type-policies} : Create a new documentTypePolicy.
     *
     * @param documentTypePolicyDTO the documentTypePolicyDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new documentTypePolicyDTO, or with status {@code 400 (Bad Request)} if the documentTypePolicy has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<DocumentTypePolicyDTO> createDocumentTypePolicy(@Valid @RequestBody DocumentTypePolicyDTO documentTypePolicyDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save DocumentTypePolicy : {}", documentTypePolicyDTO);
        if (documentTypePolicyDTO.getId() != null) {
            throw new BadRequestAlertException("A new documentTypePolicy cannot already have an ID", ENTITY_NAME, "idexists");
        }
        documentTypePolicyDTO = documentTypePolicyService.save(documentTypePolicyDTO);
        return ResponseEntity.created(new URI("/api/document-type-policies/" + documentTypePolicyDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, documentTypePolicyDTO.getId().toString()))
            .body(documentTypePolicyDTO);
    }

    /**
     * {@code PUT  /document-type-policies/:id} : Updates an existing documentTypePolicy.
     *
     * @param id the id of the documentTypePolicyDTO to save.
     * @param documentTypePolicyDTO the documentTypePolicyDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated documentTypePolicyDTO,
     * or with status {@code 400 (Bad Request)} if the documentTypePolicyDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the documentTypePolicyDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<DocumentTypePolicyDTO> updateDocumentTypePolicy(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody DocumentTypePolicyDTO documentTypePolicyDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update DocumentTypePolicy : {}, {}", id, documentTypePolicyDTO);
        if (documentTypePolicyDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, documentTypePolicyDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!documentTypePolicyRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        documentTypePolicyDTO = documentTypePolicyService.update(documentTypePolicyDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, documentTypePolicyDTO.getId().toString()))
            .body(documentTypePolicyDTO);
    }

    /**
     * {@code PATCH  /document-type-policies/:id} : Partial updates given fields of an existing documentTypePolicy, field will ignore if it is null
     *
     * @param id the id of the documentTypePolicyDTO to save.
     * @param documentTypePolicyDTO the documentTypePolicyDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated documentTypePolicyDTO,
     * or with status {@code 400 (Bad Request)} if the documentTypePolicyDTO is not valid,
     * or with status {@code 404 (Not Found)} if the documentTypePolicyDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the documentTypePolicyDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<DocumentTypePolicyDTO> partialUpdateDocumentTypePolicy(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody DocumentTypePolicyDTO documentTypePolicyDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update DocumentTypePolicy partially : {}, {}", id, documentTypePolicyDTO);
        if (documentTypePolicyDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, documentTypePolicyDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!documentTypePolicyRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<DocumentTypePolicyDTO> result = documentTypePolicyService.partialUpdate(documentTypePolicyDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, documentTypePolicyDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /document-type-policies} : get all the documentTypePolicies.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of documentTypePolicies in body.
     */
    @GetMapping("")
    public ResponseEntity<List<DocumentTypePolicyDTO>> getAllDocumentTypePolicies(DocumentTypePolicyCriteria criteria) {
        LOG.debug("REST request to get DocumentTypePolicies by criteria: {}", criteria);

        List<DocumentTypePolicyDTO> entityList = documentTypePolicyQueryService.findByCriteria(criteria);
        return ResponseEntity.ok().body(entityList);
    }

    /**
     * {@code GET  /document-type-policies/count} : count all the documentTypePolicies.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countDocumentTypePolicies(DocumentTypePolicyCriteria criteria) {
        LOG.debug("REST request to count DocumentTypePolicies by criteria: {}", criteria);
        return ResponseEntity.ok().body(documentTypePolicyQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /document-type-policies/:id} : get the "id" documentTypePolicy.
     *
     * @param id the id of the documentTypePolicyDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the documentTypePolicyDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DocumentTypePolicyDTO> getDocumentTypePolicy(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get DocumentTypePolicy : {}", id);
        Optional<DocumentTypePolicyDTO> documentTypePolicyDTO = documentTypePolicyService.findOne(id);
        return ResponseUtil.wrapOrNotFound(documentTypePolicyDTO);
    }

    /**
     * {@code DELETE  /document-type-policies/:id} : delete the "id" documentTypePolicy.
     *
     * @param id the id of the documentTypePolicyDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocumentTypePolicy(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete DocumentTypePolicy : {}", id);
        documentTypePolicyService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
