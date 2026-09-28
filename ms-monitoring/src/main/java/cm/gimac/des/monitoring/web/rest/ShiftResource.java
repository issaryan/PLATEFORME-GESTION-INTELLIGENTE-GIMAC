package cm.gimac.des.monitoring.web.rest;

import cm.gimac.des.monitoring.service.ShiftQueryService;
import cm.gimac.des.monitoring.service.ShiftService;
import cm.gimac.des.monitoring.service.criteria.ShiftCriteria;
import cm.gimac.des.monitoring.service.dto.ShiftDTO;
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
 * REST controller for managing {@link cm.gimac.des.monitoring.domain.Shift}.
 */
@RestController
@RequestMapping("/api/shifts")
public class ShiftResource {

    private static final Logger LOG = LoggerFactory.getLogger(ShiftResource.class);

    private final ShiftService shiftService;

    private final ShiftQueryService shiftQueryService;

    public ShiftResource(ShiftService shiftService, ShiftQueryService shiftQueryService) {
        this.shiftService = shiftService;
        this.shiftQueryService = shiftQueryService;
    }

    /**
     * {@code GET  /shifts} : get all the shifts.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of shifts in body.
     */
    @GetMapping("")
    public ResponseEntity<List<ShiftDTO>> getAllShifts(
        ShiftCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get Shifts by criteria: {}", criteria);

        Page<ShiftDTO> page = shiftQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /shifts/count} : count all the shifts.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countShifts(ShiftCriteria criteria) {
        LOG.debug("REST request to count Shifts by criteria: {}", criteria);
        return ResponseEntity.ok().body(shiftQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /shifts/:id} : get the "id" shift.
     *
     * @param id the id of the shiftDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the shiftDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ShiftDTO> getShift(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get Shift : {}", id);
        Optional<ShiftDTO> shiftDTO = shiftService.findOne(id);
        return ResponseUtil.wrapOrNotFound(shiftDTO);
    }
}
