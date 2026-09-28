package cm.gimac.des.monitoring.web.rest;

import cm.gimac.des.monitoring.service.ShiftReportQueryService;
import cm.gimac.des.monitoring.service.ShiftReportService;
import cm.gimac.des.monitoring.service.criteria.ShiftReportCriteria;
import cm.gimac.des.monitoring.service.dto.ShiftReportDTO;
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
 * REST controller for managing {@link cm.gimac.des.monitoring.domain.ShiftReport}.
 */
@RestController
@RequestMapping("/api/shift-reports")
public class ShiftReportResource {

    private static final Logger LOG = LoggerFactory.getLogger(ShiftReportResource.class);

    private final ShiftReportService shiftReportService;

    private final ShiftReportQueryService shiftReportQueryService;

    public ShiftReportResource(ShiftReportService shiftReportService, ShiftReportQueryService shiftReportQueryService) {
        this.shiftReportService = shiftReportService;
        this.shiftReportQueryService = shiftReportQueryService;
    }

    /**
     * {@code GET  /shift-reports} : get all the shiftReports.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of shiftReports in body.
     */
    @GetMapping("")
    public ResponseEntity<List<ShiftReportDTO>> getAllShiftReports(
        ShiftReportCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get ShiftReports by criteria: {}", criteria);

        Page<ShiftReportDTO> page = shiftReportQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /shift-reports/count} : count all the shiftReports.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countShiftReports(ShiftReportCriteria criteria) {
        LOG.debug("REST request to count ShiftReports by criteria: {}", criteria);
        return ResponseEntity.ok().body(shiftReportQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /shift-reports/:id} : get the "id" shiftReport.
     *
     * @param id the id of the shiftReportDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the shiftReportDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ShiftReportDTO> getShiftReport(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get ShiftReport : {}", id);
        Optional<ShiftReportDTO> shiftReportDTO = shiftReportService.findOne(id);
        return ResponseUtil.wrapOrNotFound(shiftReportDTO);
    }
}
