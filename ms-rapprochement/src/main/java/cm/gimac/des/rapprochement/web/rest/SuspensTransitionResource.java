package cm.gimac.des.rapprochement.web.rest;

import cm.gimac.des.rapprochement.service.SuspensTransitionQueryService;
import cm.gimac.des.rapprochement.service.SuspensTransitionService;
import cm.gimac.des.rapprochement.service.criteria.SuspensTransitionCriteria;
import cm.gimac.des.rapprochement.service.dto.SuspensTransitionDTO;
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
 * REST controller for managing {@link cm.gimac.des.rapprochement.domain.SuspensTransition}.
 */
@RestController
@RequestMapping("/api/suspens-transitions")
public class SuspensTransitionResource {

    private static final Logger LOG = LoggerFactory.getLogger(SuspensTransitionResource.class);

    private final SuspensTransitionService suspensTransitionService;

    private final SuspensTransitionQueryService suspensTransitionQueryService;

    public SuspensTransitionResource(
        SuspensTransitionService suspensTransitionService,
        SuspensTransitionQueryService suspensTransitionQueryService
    ) {
        this.suspensTransitionService = suspensTransitionService;
        this.suspensTransitionQueryService = suspensTransitionQueryService;
    }

    /**
     * {@code GET  /suspens-transitions} : get all the suspensTransitions.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of suspensTransitions in body.
     */
    @GetMapping("")
    public ResponseEntity<List<SuspensTransitionDTO>> getAllSuspensTransitions(
        SuspensTransitionCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get SuspensTransitions by criteria: {}", criteria);

        Page<SuspensTransitionDTO> page = suspensTransitionQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /suspens-transitions/count} : count all the suspensTransitions.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countSuspensTransitions(SuspensTransitionCriteria criteria) {
        LOG.debug("REST request to count SuspensTransitions by criteria: {}", criteria);
        return ResponseEntity.ok().body(suspensTransitionQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /suspens-transitions/:id} : get the "id" suspensTransition.
     *
     * @param id the id of the suspensTransitionDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the suspensTransitionDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SuspensTransitionDTO> getSuspensTransition(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get SuspensTransition : {}", id);
        Optional<SuspensTransitionDTO> suspensTransitionDTO = suspensTransitionService.findOne(id);
        return ResponseUtil.wrapOrNotFound(suspensTransitionDTO);
    }
}
