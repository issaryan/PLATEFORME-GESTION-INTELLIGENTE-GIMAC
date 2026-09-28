package cm.gimac.des.rapprochement.web.rest;

import cm.gimac.des.rapprochement.service.SuspensQueryService;
import cm.gimac.des.rapprochement.service.SuspensService;
import cm.gimac.des.rapprochement.service.criteria.SuspensCriteria;
import cm.gimac.des.rapprochement.service.dto.SuspensDTO;
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
 * REST controller for managing {@link cm.gimac.des.rapprochement.domain.Suspens}.
 */
@RestController
@RequestMapping("/api/suspens")
public class SuspensResource {

    private static final Logger LOG = LoggerFactory.getLogger(SuspensResource.class);

    private final SuspensService suspensService;

    private final SuspensQueryService suspensQueryService;

    public SuspensResource(SuspensService suspensService, SuspensQueryService suspensQueryService) {
        this.suspensService = suspensService;
        this.suspensQueryService = suspensQueryService;
    }

    /**
     * {@code GET  /suspens} : get all the suspens.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of suspens in body.
     */
    @GetMapping("")
    public ResponseEntity<List<SuspensDTO>> getAllSuspens(
        SuspensCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get Suspens by criteria: {}", criteria);

        Page<SuspensDTO> page = suspensQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /suspens/count} : count all the suspens.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countSuspens(SuspensCriteria criteria) {
        LOG.debug("REST request to count Suspens by criteria: {}", criteria);
        return ResponseEntity.ok().body(suspensQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /suspens/:id} : get the "id" suspens.
     *
     * @param id the id of the suspensDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the suspensDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SuspensDTO> getSuspens(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get Suspens : {}", id);
        Optional<SuspensDTO> suspensDTO = suspensService.findOne(id);
        return ResponseUtil.wrapOrNotFound(suspensDTO);
    }
}
