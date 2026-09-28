package cm.gimac.des.monitoring.web.rest;

import cm.gimac.des.monitoring.service.OperatorUnavailabilityQueryService;
import cm.gimac.des.monitoring.service.OperatorUnavailabilityService;
import cm.gimac.des.monitoring.service.criteria.OperatorUnavailabilityCriteria;
import cm.gimac.des.monitoring.service.dto.OperatorUnavailabilityDTO;
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
 * REST controller for managing {@link cm.gimac.des.monitoring.domain.OperatorUnavailability}.
 */
@RestController
@RequestMapping("/api/operator-unavailabilities")
public class OperatorUnavailabilityResource {

    private static final Logger LOG = LoggerFactory.getLogger(OperatorUnavailabilityResource.class);

    private final OperatorUnavailabilityService operatorUnavailabilityService;

    private final OperatorUnavailabilityQueryService operatorUnavailabilityQueryService;

    public OperatorUnavailabilityResource(
        OperatorUnavailabilityService operatorUnavailabilityService,
        OperatorUnavailabilityQueryService operatorUnavailabilityQueryService
    ) {
        this.operatorUnavailabilityService = operatorUnavailabilityService;
        this.operatorUnavailabilityQueryService = operatorUnavailabilityQueryService;
    }

    /**
     * {@code GET  /operator-unavailabilities} : get all the operatorUnavailabilities.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of operatorUnavailabilities in body.
     */
    @GetMapping("")
    public ResponseEntity<List<OperatorUnavailabilityDTO>> getAllOperatorUnavailabilities(
        OperatorUnavailabilityCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get OperatorUnavailabilities by criteria: {}", criteria);

        Page<OperatorUnavailabilityDTO> page = operatorUnavailabilityQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /operator-unavailabilities/count} : count all the operatorUnavailabilities.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countOperatorUnavailabilities(OperatorUnavailabilityCriteria criteria) {
        LOG.debug("REST request to count OperatorUnavailabilities by criteria: {}", criteria);
        return ResponseEntity.ok().body(operatorUnavailabilityQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /operator-unavailabilities/:id} : get the "id" operatorUnavailability.
     *
     * @param id the id of the operatorUnavailabilityDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the operatorUnavailabilityDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<OperatorUnavailabilityDTO> getOperatorUnavailability(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get OperatorUnavailability : {}", id);
        Optional<OperatorUnavailabilityDTO> operatorUnavailabilityDTO = operatorUnavailabilityService.findOne(id);
        return ResponseUtil.wrapOrNotFound(operatorUnavailabilityDTO);
    }
}
