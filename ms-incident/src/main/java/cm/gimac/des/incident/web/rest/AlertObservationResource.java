package cm.gimac.des.incident.web.rest;

import cm.gimac.des.incident.service.AlertObservationService;
import cm.gimac.des.incident.service.dto.AlertObservationDTO;
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
 * REST controller for managing {@link cm.gimac.des.incident.domain.AlertObservation}.
 */
@RestController
@RequestMapping("/api/alert-observations")
public class AlertObservationResource {

    private static final Logger LOG = LoggerFactory.getLogger(AlertObservationResource.class);

    private final AlertObservationService alertObservationService;

    public AlertObservationResource(AlertObservationService alertObservationService) {
        this.alertObservationService = alertObservationService;
    }

    /**
     * {@code GET  /alert-observations} : get all the alertObservations.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of alertObservations in body.
     */
    @GetMapping("")
    public ResponseEntity<List<AlertObservationDTO>> getAllAlertObservations(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get a page of AlertObservations");
        Page<AlertObservationDTO> page = alertObservationService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /alert-observations/:id} : get the "id" alertObservation.
     *
     * @param id the id of the alertObservationDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the alertObservationDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AlertObservationDTO> getAlertObservation(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get AlertObservation : {}", id);
        Optional<AlertObservationDTO> alertObservationDTO = alertObservationService.findOne(id);
        return ResponseUtil.wrapOrNotFound(alertObservationDTO);
    }
}
