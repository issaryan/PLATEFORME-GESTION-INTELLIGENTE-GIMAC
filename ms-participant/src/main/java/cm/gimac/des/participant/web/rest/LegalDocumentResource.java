package cm.gimac.des.participant.web.rest;

import cm.gimac.des.participant.repository.LegalDocumentRepository;
import cm.gimac.des.participant.service.LegalDocumentQueryService;
import cm.gimac.des.participant.service.LegalDocumentService;
import cm.gimac.des.participant.service.criteria.LegalDocumentCriteria;
import cm.gimac.des.participant.service.dto.LegalDocumentDTO;
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
 * REST controller for managing {@link cm.gimac.des.participant.domain.LegalDocument}.
 */
@RestController
@RequestMapping("/api/legal-documents")
public class LegalDocumentResource {

    private static final Logger LOG = LoggerFactory.getLogger(LegalDocumentResource.class);

    private static final String ENTITY_NAME = "participantLegalDocument";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final LegalDocumentService legalDocumentService;

    private final LegalDocumentRepository legalDocumentRepository;

    private final LegalDocumentQueryService legalDocumentQueryService;

    public LegalDocumentResource(
        LegalDocumentService legalDocumentService,
        LegalDocumentRepository legalDocumentRepository,
        LegalDocumentQueryService legalDocumentQueryService
    ) {
        this.legalDocumentService = legalDocumentService;
        this.legalDocumentRepository = legalDocumentRepository;
        this.legalDocumentQueryService = legalDocumentQueryService;
    }

    /**
     * {@code POST  /legal-documents} : Create a new legalDocument.
     *
     * @param legalDocumentDTO the legalDocumentDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new legalDocumentDTO, or with status {@code 400 (Bad Request)} if the legalDocument has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<LegalDocumentDTO> createLegalDocument(@Valid @RequestBody LegalDocumentDTO legalDocumentDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save LegalDocument : {}", legalDocumentDTO);
        if (legalDocumentDTO.getId() != null) {
            throw new BadRequestAlertException("A new legalDocument cannot already have an ID", ENTITY_NAME, "idexists");
        }
        legalDocumentDTO = legalDocumentService.save(legalDocumentDTO);
        return ResponseEntity.created(new URI("/api/legal-documents/" + legalDocumentDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, legalDocumentDTO.getId().toString()))
            .body(legalDocumentDTO);
    }

    /**
     * {@code PUT  /legal-documents/:id} : Updates an existing legalDocument.
     *
     * @param id the id of the legalDocumentDTO to save.
     * @param legalDocumentDTO the legalDocumentDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated legalDocumentDTO,
     * or with status {@code 400 (Bad Request)} if the legalDocumentDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the legalDocumentDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<LegalDocumentDTO> updateLegalDocument(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody LegalDocumentDTO legalDocumentDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update LegalDocument : {}, {}", id, legalDocumentDTO);
        if (legalDocumentDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, legalDocumentDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!legalDocumentRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        legalDocumentDTO = legalDocumentService.update(legalDocumentDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, legalDocumentDTO.getId().toString()))
            .body(legalDocumentDTO);
    }

    /**
     * {@code PATCH  /legal-documents/:id} : Partial updates given fields of an existing legalDocument, field will ignore if it is null
     *
     * @param id the id of the legalDocumentDTO to save.
     * @param legalDocumentDTO the legalDocumentDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated legalDocumentDTO,
     * or with status {@code 400 (Bad Request)} if the legalDocumentDTO is not valid,
     * or with status {@code 404 (Not Found)} if the legalDocumentDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the legalDocumentDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<LegalDocumentDTO> partialUpdateLegalDocument(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody LegalDocumentDTO legalDocumentDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update LegalDocument partially : {}, {}", id, legalDocumentDTO);
        if (legalDocumentDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, legalDocumentDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!legalDocumentRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<LegalDocumentDTO> result = legalDocumentService.partialUpdate(legalDocumentDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, legalDocumentDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /legal-documents} : get all the legalDocuments.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of legalDocuments in body.
     */
    @GetMapping("")
    public ResponseEntity<List<LegalDocumentDTO>> getAllLegalDocuments(
        LegalDocumentCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get LegalDocuments by criteria: {}", criteria);

        Page<LegalDocumentDTO> page = legalDocumentQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /legal-documents/count} : count all the legalDocuments.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countLegalDocuments(LegalDocumentCriteria criteria) {
        LOG.debug("REST request to count LegalDocuments by criteria: {}", criteria);
        return ResponseEntity.ok().body(legalDocumentQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /legal-documents/:id} : get the "id" legalDocument.
     *
     * @param id the id of the legalDocumentDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the legalDocumentDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<LegalDocumentDTO> getLegalDocument(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get LegalDocument : {}", id);
        Optional<LegalDocumentDTO> legalDocumentDTO = legalDocumentService.findOne(id);
        return ResponseUtil.wrapOrNotFound(legalDocumentDTO);
    }

    /**
     * {@code DELETE  /legal-documents/:id} : delete the "id" legalDocument.
     *
     * @param id the id of the legalDocumentDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLegalDocument(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete LegalDocument : {}", id);
        legalDocumentService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
