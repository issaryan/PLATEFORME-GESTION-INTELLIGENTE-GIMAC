package cm.gimac.des.monitoring.web.rest;

import cm.gimac.des.monitoring.service.ShiftLogEntryQueryService;
import cm.gimac.des.monitoring.service.ShiftLogEntryService;
import cm.gimac.des.monitoring.service.criteria.ShiftLogEntryCriteria;
import cm.gimac.des.monitoring.service.dto.ShiftLogEntryDTO;
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
 * REST controller for managing {@link cm.gimac.des.monitoring.domain.ShiftLogEntry}.
 */
@RestController
@RequestMapping("/api/shift-log-entries")
public class ShiftLogEntryResource {

    private static final Logger LOG = LoggerFactory.getLogger(ShiftLogEntryResource.class);

    private final ShiftLogEntryService shiftLogEntryService;

    private final ShiftLogEntryQueryService shiftLogEntryQueryService;

    public ShiftLogEntryResource(ShiftLogEntryService shiftLogEntryService, ShiftLogEntryQueryService shiftLogEntryQueryService) {
        this.shiftLogEntryService = shiftLogEntryService;
        this.shiftLogEntryQueryService = shiftLogEntryQueryService;
    }

    /**
     * {@code GET  /shift-log-entries} : get all the shiftLogEntries.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of shiftLogEntries in body.
     */
    @GetMapping("")
    public ResponseEntity<List<ShiftLogEntryDTO>> getAllShiftLogEntries(
        ShiftLogEntryCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get ShiftLogEntries by criteria: {}", criteria);

        Page<ShiftLogEntryDTO> page = shiftLogEntryQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /shift-log-entries/count} : count all the shiftLogEntries.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countShiftLogEntries(ShiftLogEntryCriteria criteria) {
        LOG.debug("REST request to count ShiftLogEntries by criteria: {}", criteria);
        return ResponseEntity.ok().body(shiftLogEntryQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /shift-log-entries/:id} : get the "id" shiftLogEntry.
     *
     * @param id the id of the shiftLogEntryDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the shiftLogEntryDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ShiftLogEntryDTO> getShiftLogEntry(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get ShiftLogEntry : {}", id);
        Optional<ShiftLogEntryDTO> shiftLogEntryDTO = shiftLogEntryService.findOne(id);
        return ResponseUtil.wrapOrNotFound(shiftLogEntryDTO);
    }
}
