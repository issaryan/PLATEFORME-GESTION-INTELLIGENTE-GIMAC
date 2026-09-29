package cm.gimac.des.participant.web.rest;

import cm.gimac.des.participant.service.DeadlineQueryService;
import cm.gimac.des.participant.service.DeadlineService;
import cm.gimac.des.participant.service.criteria.DeadlineCriteria;
import cm.gimac.des.participant.service.dto.DeadlineDTO;
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
 * REST controller for managing {@link cm.gimac.des.participant.domain.Deadline}.
 */
@RestController
@RequestMapping("/api/deadlines")
public class DeadlineResource {

    private static final Logger LOG = LoggerFactory.getLogger(DeadlineResource.class);

    private final DeadlineService deadlineService;

    private final DeadlineQueryService deadlineQueryService;

    public DeadlineResource(DeadlineService deadlineService, DeadlineQueryService deadlineQueryService) {
        this.deadlineService = deadlineService;
        this.deadlineQueryService = deadlineQueryService;
    }

    /**
     * {@code GET  /deadlines} : get all the Deadlines.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Deadlines in body.
     */
    @GetMapping("")
    public ResponseEntity<List<DeadlineDTO>> getAllDeadlines(
        DeadlineCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get Deadlines by criteria: {}", criteria);

        Page<DeadlineDTO> page = deadlineQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /deadlines/count} : count all the deadlines.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countDeadlines(DeadlineCriteria criteria) {
        LOG.debug("REST request to count Deadlines by criteria: {}", criteria);
        return ResponseEntity.ok().body(deadlineQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /deadlines/:id} : get the "id" deadline.
     *
     * @param id the id of the deadlineDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the deadlineDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DeadlineDTO> getDeadline(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get Deadline : {}", id);
        Optional<DeadlineDTO> deadlineDTO = deadlineService.findOne(id);
        return ResponseUtil.wrapOrNotFound(deadlineDTO);
    }
}
