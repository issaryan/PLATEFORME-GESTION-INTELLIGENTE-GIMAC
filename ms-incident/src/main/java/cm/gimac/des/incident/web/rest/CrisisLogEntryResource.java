package cm.gimac.des.incident.web.rest;

import cm.gimac.des.incident.service.CrisisLogEntryQueryService;
import cm.gimac.des.incident.service.CrisisLogEntryService;
import cm.gimac.des.incident.service.criteria.CrisisLogEntryCriteria;
import cm.gimac.des.incident.service.dto.CrisisLogEntryDTO;
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
 * REST controller for managing {@link cm.gimac.des.incident.domain.CrisisLogEntry}.
 */
@RestController
@RequestMapping("/api/crisis-log-entries")
public class CrisisLogEntryResource {

    private static final Logger LOG = LoggerFactory.getLogger(CrisisLogEntryResource.class);

    private final CrisisLogEntryService crisisLogEntryService;

    private final CrisisLogEntryQueryService crisisLogEntryQueryService;

    public CrisisLogEntryResource(CrisisLogEntryService crisisLogEntryService, CrisisLogEntryQueryService crisisLogEntryQueryService) {
        this.crisisLogEntryService = crisisLogEntryService;
        this.crisisLogEntryQueryService = crisisLogEntryQueryService;
    }

    /**
     * {@code GET  /crisis-log-entries} : get all the crisisLogEntries.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of crisisLogEntries in body.
     */
    @GetMapping("")
    public ResponseEntity<List<CrisisLogEntryDTO>> getAllCrisisLogEntries(
        CrisisLogEntryCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get CrisisLogEntries by criteria: {}", criteria);

        Page<CrisisLogEntryDTO> page = crisisLogEntryQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /crisis-log-entries/count} : count all the crisisLogEntries.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countCrisisLogEntries(CrisisLogEntryCriteria criteria) {
        LOG.debug("REST request to count CrisisLogEntries by criteria: {}", criteria);
        return ResponseEntity.ok().body(crisisLogEntryQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /crisis-log-entries/:id} : get the "id" crisisLogEntry.
     *
     * @param id the id of the crisisLogEntryDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the crisisLogEntryDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CrisisLogEntryDTO> getCrisisLogEntry(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get CrisisLogEntry : {}", id);
        Optional<CrisisLogEntryDTO> crisisLogEntryDTO = crisisLogEntryService.findOne(id);
        return ResponseUtil.wrapOrNotFound(crisisLogEntryDTO);
    }
}
