package cm.gimac.des.document.web.rest;

import cm.gimac.des.document.service.IntegrityMismatchService;
import cm.gimac.des.document.service.dto.IntegrityMismatchDTO;
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
 * REST controller for managing {@link cm.gimac.des.document.domain.IntegrityMismatch}.
 */
@RestController
@RequestMapping("/api/integrity-mismatches")
public class IntegrityMismatchResource {

    private static final Logger LOG = LoggerFactory.getLogger(IntegrityMismatchResource.class);

    private final IntegrityMismatchService integrityMismatchService;

    public IntegrityMismatchResource(IntegrityMismatchService integrityMismatchService) {
        this.integrityMismatchService = integrityMismatchService;
    }

    /**
     * {@code GET  /integrity-mismatches} : get all the integrityMismatches.
     *
     * @param pageable the pagination information.
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of integrityMismatches in body.
     */
    @GetMapping("")
    public ResponseEntity<List<IntegrityMismatchDTO>> getAllIntegrityMismatches(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get a page of IntegrityMismatches");
        Page<IntegrityMismatchDTO> page;
        if (eagerload) {
            page = integrityMismatchService.findAllWithEagerRelationships(pageable);
        } else {
            page = integrityMismatchService.findAll(pageable);
        }
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /integrity-mismatches/:id} : get the "id" integrityMismatch.
     *
     * @param id the id of the integrityMismatchDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the integrityMismatchDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<IntegrityMismatchDTO> getIntegrityMismatch(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get IntegrityMismatch : {}", id);
        Optional<IntegrityMismatchDTO> integrityMismatchDTO = integrityMismatchService.findOne(id);
        return ResponseUtil.wrapOrNotFound(integrityMismatchDTO);
    }
}
