package cm.gimac.des.notification.web.rest;

import cm.gimac.des.notification.service.DeliveryTraceQueryService;
import cm.gimac.des.notification.service.DeliveryTraceService;
import cm.gimac.des.notification.service.criteria.DeliveryTraceCriteria;
import cm.gimac.des.notification.service.dto.DeliveryTraceDTO;
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
 * REST controller for managing {@link cm.gimac.des.notification.domain.DeliveryTrace}.
 */
@RestController
@RequestMapping("/api/delivery-traces")
public class DeliveryTraceResource {

    private static final Logger LOG = LoggerFactory.getLogger(DeliveryTraceResource.class);

    private final DeliveryTraceService deliveryTraceService;

    private final DeliveryTraceQueryService deliveryTraceQueryService;

    public DeliveryTraceResource(DeliveryTraceService deliveryTraceService, DeliveryTraceQueryService deliveryTraceQueryService) {
        this.deliveryTraceService = deliveryTraceService;
        this.deliveryTraceQueryService = deliveryTraceQueryService;
    }

    /**
     * {@code GET  /delivery-traces} : get all the deliveryTraces.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of deliveryTraces in body.
     */
    @GetMapping("")
    public ResponseEntity<List<DeliveryTraceDTO>> getAllDeliveryTraces(
        DeliveryTraceCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get DeliveryTraces by criteria: {}", criteria);

        Page<DeliveryTraceDTO> page = deliveryTraceQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /delivery-traces/count} : count all the deliveryTraces.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countDeliveryTraces(DeliveryTraceCriteria criteria) {
        LOG.debug("REST request to count DeliveryTraces by criteria: {}", criteria);
        return ResponseEntity.ok().body(deliveryTraceQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /delivery-traces/:id} : get the "id" deliveryTrace.
     *
     * @param id the id of the deliveryTraceDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the deliveryTraceDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DeliveryTraceDTO> getDeliveryTrace(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get DeliveryTrace : {}", id);
        Optional<DeliveryTraceDTO> deliveryTraceDTO = deliveryTraceService.findOne(id);
        return ResponseUtil.wrapOrNotFound(deliveryTraceDTO);
    }
}
