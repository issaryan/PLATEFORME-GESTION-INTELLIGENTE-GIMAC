package cm.gimac.des.rapprochement.web.rest;

import cm.gimac.des.rapprochement.service.ReconciliationQueryService;
import cm.gimac.des.rapprochement.service.ReconciliationService;
import cm.gimac.des.rapprochement.service.criteria.ReconciliationCriteria;
import cm.gimac.des.rapprochement.service.dto.ReconciliationDTO;
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
 * REST controller for managing {@link cm.gimac.des.rapprochement.domain.Reconciliation}.
 */
@RestController
@RequestMapping("/api/reconciliations")
public class ReconciliationResource {

    private static final Logger LOG = LoggerFactory.getLogger(ReconciliationResource.class);

    private final ReconciliationService reconciliationService;

    private final ReconciliationQueryService reconciliationQueryService;

    public ReconciliationResource(ReconciliationService reconciliationService, ReconciliationQueryService reconciliationQueryService) {
        this.reconciliationService = reconciliationService;
        this.reconciliationQueryService = reconciliationQueryService;
    }

    /**
     * {@code GET  /reconciliations} : get all the reconciliations.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of reconciliations in body.
     */
    @GetMapping("")
    public ResponseEntity<List<ReconciliationDTO>> getAllReconciliations(
        ReconciliationCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get Reconciliations by criteria: {}", criteria);

        Page<ReconciliationDTO> page = reconciliationQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /reconciliations/count} : count all the reconciliations.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countReconciliations(ReconciliationCriteria criteria) {
        LOG.debug("REST request to count Reconciliations by criteria: {}", criteria);
        return ResponseEntity.ok().body(reconciliationQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /reconciliations/:id} : get the "id" reconciliation.
     *
     * @param id the id of the reconciliationDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the reconciliationDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReconciliationDTO> getReconciliation(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get Reconciliation : {}", id);
        Optional<ReconciliationDTO> reconciliationDTO = reconciliationService.findOne(id);
        return ResponseUtil.wrapOrNotFound(reconciliationDTO);
    }
}
