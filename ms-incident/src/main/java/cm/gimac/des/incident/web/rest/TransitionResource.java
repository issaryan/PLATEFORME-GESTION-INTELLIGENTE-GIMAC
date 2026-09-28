package cm.gimac.des.incident.web.rest;

import cm.gimac.des.incident.service.TransitionQueryService;
import cm.gimac.des.incident.service.TransitionService;
import cm.gimac.des.incident.service.criteria.TransitionCriteria;
import cm.gimac.des.incident.service.dto.TransitionDTO;
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
 * REST controller for managing {@link cm.gimac.des.incident.domain.Transition}.
 */
@RestController
@RequestMapping("/api/transitions")
public class TransitionResource {

    private static final Logger LOG = LoggerFactory.getLogger(TransitionResource.class);

    private final TransitionService transitionService;

    private final TransitionQueryService transitionQueryService;

    public TransitionResource(TransitionService transitionService, TransitionQueryService transitionQueryService) {
        this.transitionService = transitionService;
        this.transitionQueryService = transitionQueryService;
    }

    /**
     * {@code GET  /transitions} : get all the transitions.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of transitions in body.
     */
    @GetMapping("")
    public ResponseEntity<List<TransitionDTO>> getAllTransitions(
        TransitionCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get Transitions by criteria: {}", criteria);

        Page<TransitionDTO> page = transitionQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /transitions/count} : count all the transitions.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countTransitions(TransitionCriteria criteria) {
        LOG.debug("REST request to count Transitions by criteria: {}", criteria);
        return ResponseEntity.ok().body(transitionQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /transitions/:id} : get the "id" transition.
     *
     * @param id the id of the transitionDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the transitionDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<TransitionDTO> getTransition(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get Transition : {}", id);
        Optional<TransitionDTO> transitionDTO = transitionService.findOne(id);
        return ResponseUtil.wrapOrNotFound(transitionDTO);
    }
}
