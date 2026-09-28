package cm.gimac.des.document.web.rest;

import cm.gimac.des.document.service.DocumentLineageQueryService;
import cm.gimac.des.document.service.DocumentLineageService;
import cm.gimac.des.document.service.criteria.DocumentLineageCriteria;
import cm.gimac.des.document.service.dto.DocumentLineageDTO;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link cm.gimac.des.document.domain.DocumentLineage}.
 */
@RestController
@RequestMapping("/api/document-lineages")
public class DocumentLineageResource {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentLineageResource.class);

    private final DocumentLineageService documentLineageService;

    private final DocumentLineageQueryService documentLineageQueryService;

    public DocumentLineageResource(DocumentLineageService documentLineageService, DocumentLineageQueryService documentLineageQueryService) {
        this.documentLineageService = documentLineageService;
        this.documentLineageQueryService = documentLineageQueryService;
    }

    /**
     * {@code GET  /document-lineages} : get all the documentLineages.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of documentLineages in body.
     */
    @GetMapping("")
    public ResponseEntity<List<DocumentLineageDTO>> getAllDocumentLineages(
        DocumentLineageCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get DocumentLineages by criteria: {}", criteria);

        Page<DocumentLineageDTO> page = documentLineageQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /document-lineages/count} : count all the documentLineages.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countDocumentLineages(DocumentLineageCriteria criteria) {
        LOG.debug("REST request to count DocumentLineages by criteria: {}", criteria);
        return ResponseEntity.ok().body(documentLineageQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /document-lineages/:id} : get the "id" documentLineage.
     *
     * @param id the id of the documentLineageDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the documentLineageDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DocumentLineageDTO> getDocumentLineage(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get DocumentLineage : {}", id);
        Optional<DocumentLineageDTO> documentLineageDTO = documentLineageService.findOne(id);
        return ResponseUtil.wrapOrNotFound(documentLineageDTO);
    }
}
