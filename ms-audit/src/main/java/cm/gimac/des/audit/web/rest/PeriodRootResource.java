package cm.gimac.des.audit.web.rest;

import cm.gimac.des.audit.service.PeriodRootQueryService;
import cm.gimac.des.audit.service.PeriodRootService;
import cm.gimac.des.audit.service.criteria.PeriodRootCriteria;
import cm.gimac.des.audit.service.dto.PeriodRootDTO;
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
 * REST controller for managing {@link cm.gimac.des.audit.domain.PeriodRoot}.
 */
@RestController
@RequestMapping("/api/period-roots")
public class PeriodRootResource {

    private static final Logger LOG = LoggerFactory.getLogger(PeriodRootResource.class);

    private final PeriodRootService periodRootService;

    private final PeriodRootQueryService periodRootQueryService;

    public PeriodRootResource(PeriodRootService periodRootService, PeriodRootQueryService periodRootQueryService) {
        this.periodRootService = periodRootService;
        this.periodRootQueryService = periodRootQueryService;
    }

    /**
     * {@code GET  /period-roots} : get all the Period Roots.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Period Roots in body.
     */
    @GetMapping("")
    public ResponseEntity<List<PeriodRootDTO>> getAllPeriodRoots(
        PeriodRootCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get PeriodRoots by criteria: {}", criteria);

        Page<PeriodRootDTO> page = periodRootQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /period-roots/count} : count all the periodRoots.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countPeriodRoots(PeriodRootCriteria criteria) {
        LOG.debug("REST request to count PeriodRoots by criteria: {}", criteria);
        return ResponseEntity.ok().body(periodRootQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /period-roots/:id} : get the "id" periodRoot.
     *
     * @param id the id of the periodRootDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the periodRootDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PeriodRootDTO> getPeriodRoot(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get PeriodRoot : {}", id);
        Optional<PeriodRootDTO> periodRootDTO = periodRootService.findOne(id);
        return ResponseUtil.wrapOrNotFound(periodRootDTO);
    }
}
