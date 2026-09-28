package cm.gimac.des.rapprochement.web.rest;

import cm.gimac.des.rapprochement.service.ConciliationEntryService;
import cm.gimac.des.rapprochement.service.dto.ConciliationEntryDTO;
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
 * REST controller for managing {@link cm.gimac.des.rapprochement.domain.ConciliationEntry}.
 */
@RestController
@RequestMapping("/api/conciliation-entries")
public class ConciliationEntryResource {

    private static final Logger LOG = LoggerFactory.getLogger(ConciliationEntryResource.class);

    private final ConciliationEntryService conciliationEntryService;

    public ConciliationEntryResource(ConciliationEntryService conciliationEntryService) {
        this.conciliationEntryService = conciliationEntryService;
    }

    /**
     * {@code GET  /conciliation-entries} : get all the conciliationEntries.
     *
     * @param pageable the pagination information.
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of conciliationEntries in body.
     */
    @GetMapping("")
    public ResponseEntity<List<ConciliationEntryDTO>> getAllConciliationEntries(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get a page of ConciliationEntries");
        Page<ConciliationEntryDTO> page;
        if (eagerload) {
            page = conciliationEntryService.findAllWithEagerRelationships(pageable);
        } else {
            page = conciliationEntryService.findAll(pageable);
        }
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /conciliation-entries/:id} : get the "id" conciliationEntry.
     *
     * @param id the id of the conciliationEntryDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the conciliationEntryDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ConciliationEntryDTO> getConciliationEntry(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get ConciliationEntry : {}", id);
        Optional<ConciliationEntryDTO> conciliationEntryDTO = conciliationEntryService.findOne(id);
        return ResponseUtil.wrapOrNotFound(conciliationEntryDTO);
    }
}
