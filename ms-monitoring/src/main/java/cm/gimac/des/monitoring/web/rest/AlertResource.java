package cm.gimac.des.monitoring.web.rest;

import cm.gimac.des.monitoring.service.AlertQueryService;
import cm.gimac.des.monitoring.service.AlertService;
import cm.gimac.des.monitoring.service.criteria.AlertCriteria;
import cm.gimac.des.monitoring.service.dto.AlertDTO;
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
 * REST controller for managing {@link cm.gimac.des.monitoring.domain.Alert}.
 */
@RestController
@RequestMapping("/api/alerts")
public class AlertResource {

    private static final Logger LOG = LoggerFactory.getLogger(AlertResource.class);

    private final AlertService alertService;

    private final AlertQueryService alertQueryService;

    public AlertResource(AlertService alertService, AlertQueryService alertQueryService) {
        this.alertService = alertService;
        this.alertQueryService = alertQueryService;
    }

    /**
     * {@code GET  /alerts} : get all the alerts.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of alerts in body.
     */
    @GetMapping("")
    public ResponseEntity<List<AlertDTO>> getAllAlerts(
        AlertCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get Alerts by criteria: {}", criteria);

        Page<AlertDTO> page = alertQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /alerts/count} : count all the alerts.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countAlerts(AlertCriteria criteria) {
        LOG.debug("REST request to count Alerts by criteria: {}", criteria);
        return ResponseEntity.ok().body(alertQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /alerts/:id} : get the "id" alert.
     *
     * @param id the id of the alertDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the alertDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AlertDTO> getAlert(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get Alert : {}", id);
        Optional<AlertDTO> alertDTO = alertService.findOne(id);
        return ResponseUtil.wrapOrNotFound(alertDTO);
    }
}
