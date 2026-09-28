package cm.gimac.des.monitoring.web.rest;

import cm.gimac.des.monitoring.service.OnCallAssignmentQueryService;
import cm.gimac.des.monitoring.service.OnCallAssignmentService;
import cm.gimac.des.monitoring.service.criteria.OnCallAssignmentCriteria;
import cm.gimac.des.monitoring.service.dto.OnCallAssignmentDTO;
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
 * REST controller for managing {@link cm.gimac.des.monitoring.domain.OnCallAssignment}.
 */
@RestController
@RequestMapping("/api/on-call-assignments")
public class OnCallAssignmentResource {

    private static final Logger LOG = LoggerFactory.getLogger(OnCallAssignmentResource.class);

    private final OnCallAssignmentService onCallAssignmentService;

    private final OnCallAssignmentQueryService onCallAssignmentQueryService;

    public OnCallAssignmentResource(
        OnCallAssignmentService onCallAssignmentService,
        OnCallAssignmentQueryService onCallAssignmentQueryService
    ) {
        this.onCallAssignmentService = onCallAssignmentService;
        this.onCallAssignmentQueryService = onCallAssignmentQueryService;
    }

    /**
     * {@code GET  /on-call-assignments} : get all the onCallAssignments.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of onCallAssignments in body.
     */
    @GetMapping("")
    public ResponseEntity<List<OnCallAssignmentDTO>> getAllOnCallAssignments(
        OnCallAssignmentCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get OnCallAssignments by criteria: {}", criteria);

        Page<OnCallAssignmentDTO> page = onCallAssignmentQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /on-call-assignments/count} : count all the onCallAssignments.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countOnCallAssignments(OnCallAssignmentCriteria criteria) {
        LOG.debug("REST request to count OnCallAssignments by criteria: {}", criteria);
        return ResponseEntity.ok().body(onCallAssignmentQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /on-call-assignments/:id} : get the "id" onCallAssignment.
     *
     * @param id the id of the onCallAssignmentDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the onCallAssignmentDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<OnCallAssignmentDTO> getOnCallAssignment(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get OnCallAssignment : {}", id);
        Optional<OnCallAssignmentDTO> onCallAssignmentDTO = onCallAssignmentService.findOne(id);
        return ResponseUtil.wrapOrNotFound(onCallAssignmentDTO);
    }
}
