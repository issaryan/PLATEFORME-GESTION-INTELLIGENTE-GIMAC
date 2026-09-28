package cm.gimac.des.audit.web.rest;

import cm.gimac.des.audit.service.AuditEventQueryService;
import cm.gimac.des.audit.service.AuditEventService;
import cm.gimac.des.audit.service.criteria.AuditEventCriteria;
import cm.gimac.des.audit.service.dto.AuditEventDTO;
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
 * REST controller for managing {@link cm.gimac.des.audit.domain.AuditEvent}.
 */
@RestController
@RequestMapping("/api/audit-events")
public class AuditEventResource {

    private static final Logger LOG = LoggerFactory.getLogger(AuditEventResource.class);

    private final AuditEventService auditEventService;

    private final AuditEventQueryService auditEventQueryService;

    public AuditEventResource(AuditEventService auditEventService, AuditEventQueryService auditEventQueryService) {
        this.auditEventService = auditEventService;
        this.auditEventQueryService = auditEventQueryService;
    }

    /**
     * {@code GET  /audit-events} : get all the Audit Events.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Audit Events in body.
     */
    @GetMapping("")
    public ResponseEntity<List<AuditEventDTO>> getAllAuditEvents(
        AuditEventCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get AuditEvents by criteria: {}", criteria);

        Page<AuditEventDTO> page = auditEventQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /audit-events/count} : count all the auditEvents.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countAuditEvents(AuditEventCriteria criteria) {
        LOG.debug("REST request to count AuditEvents by criteria: {}", criteria);
        return ResponseEntity.ok().body(auditEventQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /audit-events/:id} : get the "id" auditEvent.
     *
     * @param id the id of the auditEventDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the auditEventDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AuditEventDTO> getAuditEvent(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get AuditEvent : {}", id);
        Optional<AuditEventDTO> auditEventDTO = auditEventService.findOne(id);
        return ResponseUtil.wrapOrNotFound(auditEventDTO);
    }
}
