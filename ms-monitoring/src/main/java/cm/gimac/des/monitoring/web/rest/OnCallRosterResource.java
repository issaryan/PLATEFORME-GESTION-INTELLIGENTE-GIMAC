package cm.gimac.des.monitoring.web.rest;

import cm.gimac.des.monitoring.service.OnCallRosterService;
import cm.gimac.des.monitoring.service.dto.OnCallRosterDTO;
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
 * REST controller for managing {@link cm.gimac.des.monitoring.domain.OnCallRoster}.
 */
@RestController
@RequestMapping("/api/on-call-rosters")
public class OnCallRosterResource {

    private static final Logger LOG = LoggerFactory.getLogger(OnCallRosterResource.class);

    private final OnCallRosterService onCallRosterService;

    public OnCallRosterResource(OnCallRosterService onCallRosterService) {
        this.onCallRosterService = onCallRosterService;
    }

    /**
     * {@code GET  /on-call-rosters} : get all the onCallRosters.
     *
     * @param pageable the pagination information.
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of onCallRosters in body.
     */
    @GetMapping("")
    public ResponseEntity<List<OnCallRosterDTO>> getAllOnCallRosters(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get a page of OnCallRosters");
        Page<OnCallRosterDTO> page;
        if (eagerload) {
            page = onCallRosterService.findAllWithEagerRelationships(pageable);
        } else {
            page = onCallRosterService.findAll(pageable);
        }
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /on-call-rosters/:id} : get the "id" onCallRoster.
     *
     * @param id the id of the onCallRosterDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the onCallRosterDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<OnCallRosterDTO> getOnCallRoster(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get OnCallRoster : {}", id);
        Optional<OnCallRosterDTO> onCallRosterDTO = onCallRosterService.findOne(id);
        return ResponseUtil.wrapOrNotFound(onCallRosterDTO);
    }
}
