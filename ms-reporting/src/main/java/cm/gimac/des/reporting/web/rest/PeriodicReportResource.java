package cm.gimac.des.reporting.web.rest;

import cm.gimac.des.reporting.service.PeriodicReportQueryService;
import cm.gimac.des.reporting.service.PeriodicReportService;
import cm.gimac.des.reporting.service.criteria.PeriodicReportCriteria;
import cm.gimac.des.reporting.service.dto.PeriodicReportDTO;
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
 * REST controller for managing {@link cm.gimac.des.reporting.domain.PeriodicReport}.
 */
@RestController
@RequestMapping("/api/periodic-reports")
public class PeriodicReportResource {

    private static final Logger LOG = LoggerFactory.getLogger(PeriodicReportResource.class);

    private final PeriodicReportService periodicReportService;

    private final PeriodicReportQueryService periodicReportQueryService;

    public PeriodicReportResource(PeriodicReportService periodicReportService, PeriodicReportQueryService periodicReportQueryService) {
        this.periodicReportService = periodicReportService;
        this.periodicReportQueryService = periodicReportQueryService;
    }

    /**
     * {@code GET  /periodic-reports} : get all the periodicReports.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of periodicReports in body.
     */
    @GetMapping("")
    public ResponseEntity<List<PeriodicReportDTO>> getAllPeriodicReports(
        PeriodicReportCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get PeriodicReports by criteria: {}", criteria);

        Page<PeriodicReportDTO> page = periodicReportQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /periodic-reports/count} : count all the periodicReports.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countPeriodicReports(PeriodicReportCriteria criteria) {
        LOG.debug("REST request to count PeriodicReports by criteria: {}", criteria);
        return ResponseEntity.ok().body(periodicReportQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /periodic-reports/:id} : get the "id" periodicReport.
     *
     * @param id the id of the periodicReportDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the periodicReportDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PeriodicReportDTO> getPeriodicReport(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get PeriodicReport : {}", id);
        Optional<PeriodicReportDTO> periodicReportDTO = periodicReportService.findOne(id);
        return ResponseUtil.wrapOrNotFound(periodicReportDTO);
    }
}
