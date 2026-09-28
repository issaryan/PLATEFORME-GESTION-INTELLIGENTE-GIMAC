package cm.gimac.des.homologation.web.rest;

import cm.gimac.des.homologation.service.OperationQueryService;
import cm.gimac.des.homologation.service.OperationService;
import cm.gimac.des.homologation.service.criteria.OperationCriteria;
import cm.gimac.des.homologation.service.dto.OperationDTO;
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
 * REST controller for managing {@link cm.gimac.des.homologation.domain.Operation}.
 */
@RestController
@RequestMapping("/api/operations")
public class OperationResource {

    private static final Logger LOG = LoggerFactory.getLogger(OperationResource.class);

    private final OperationService operationService;

    private final OperationQueryService operationQueryService;

    public OperationResource(OperationService operationService, OperationQueryService operationQueryService) {
        this.operationService = operationService;
        this.operationQueryService = operationQueryService;
    }

    /**
     * {@code GET  /operations} : get all the operations.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of operations in body.
     */
    @GetMapping("")
    public ResponseEntity<List<OperationDTO>> getAllOperations(
        OperationCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get Operations by criteria: {}", criteria);

        Page<OperationDTO> page = operationQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /operations/count} : count all the operations.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countOperations(OperationCriteria criteria) {
        LOG.debug("REST request to count Operations by criteria: {}", criteria);
        return ResponseEntity.ok().body(operationQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /operations/:id} : get the "id" operation.
     *
     * @param id the id of the operationDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the operationDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<OperationDTO> getOperation(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get Operation : {}", id);
        Optional<OperationDTO> operationDTO = operationService.findOne(id);
        return ResponseUtil.wrapOrNotFound(operationDTO);
    }
}
