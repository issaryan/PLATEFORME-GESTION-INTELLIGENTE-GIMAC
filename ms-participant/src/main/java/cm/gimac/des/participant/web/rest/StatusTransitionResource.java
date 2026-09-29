package cm.gimac.des.participant.web.rest;

import cm.gimac.des.participant.service.StatusTransitionQueryService;
import cm.gimac.des.participant.service.StatusTransitionService;
import cm.gimac.des.participant.service.criteria.StatusTransitionCriteria;
import cm.gimac.des.participant.service.dto.StatusTransitionDTO;
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
 * REST controller for managing {@link cm.gimac.des.participant.domain.StatusTransition}.
 */
@RestController
@RequestMapping("/api/status-transitions")
public class StatusTransitionResource {

    private static final Logger LOG = LoggerFactory.getLogger(StatusTransitionResource.class);

    private final StatusTransitionService statusTransitionService;

    private final StatusTransitionQueryService statusTransitionQueryService;

    public StatusTransitionResource(
        StatusTransitionService statusTransitionService,
        StatusTransitionQueryService statusTransitionQueryService
    ) {
        this.statusTransitionService = statusTransitionService;
        this.statusTransitionQueryService = statusTransitionQueryService;
    }

    /**
     * {@code GET  /status-transitions} : get all the Status Transitions.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Status Transitions in body.
     */
    @GetMapping("")
    public ResponseEntity<List<StatusTransitionDTO>> getAllStatusTransitions(
        StatusTransitionCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get StatusTransitions by criteria: {}", criteria);

        Page<StatusTransitionDTO> page = statusTransitionQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /status-transitions/count} : count all the statusTransitions.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countStatusTransitions(StatusTransitionCriteria criteria) {
        LOG.debug("REST request to count StatusTransitions by criteria: {}", criteria);
        return ResponseEntity.ok().body(statusTransitionQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /status-transitions/:id} : get the "id" statusTransition.
     *
     * @param id the id of the statusTransitionDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the statusTransitionDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<StatusTransitionDTO> getStatusTransition(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get StatusTransition : {}", id);
        Optional<StatusTransitionDTO> statusTransitionDTO = statusTransitionService.findOne(id);
        return ResponseUtil.wrapOrNotFound(statusTransitionDTO);
    }
}
