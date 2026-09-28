package cm.gimac.des.incident.web.rest;

import cm.gimac.des.incident.service.EscalationQueryService;
import cm.gimac.des.incident.service.EscalationService;
import cm.gimac.des.incident.service.criteria.EscalationCriteria;
import cm.gimac.des.incident.service.dto.EscalationDTO;
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
 * REST controller for managing {@link cm.gimac.des.incident.domain.Escalation}.
 */
@RestController
@RequestMapping("/api/escalations")
public class EscalationResource {

    private static final Logger LOG = LoggerFactory.getLogger(EscalationResource.class);

    private final EscalationService escalationService;

    private final EscalationQueryService escalationQueryService;

    public EscalationResource(EscalationService escalationService, EscalationQueryService escalationQueryService) {
        this.escalationService = escalationService;
        this.escalationQueryService = escalationQueryService;
    }

    /**
     * {@code GET  /escalations} : get all the escalations.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of escalations in body.
     */
    @GetMapping("")
    public ResponseEntity<List<EscalationDTO>> getAllEscalations(
        EscalationCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get Escalations by criteria: {}", criteria);

        Page<EscalationDTO> page = escalationQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /escalations/count} : count all the escalations.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countEscalations(EscalationCriteria criteria) {
        LOG.debug("REST request to count Escalations by criteria: {}", criteria);
        return ResponseEntity.ok().body(escalationQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /escalations/:id} : get the "id" escalation.
     *
     * @param id the id of the escalationDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the escalationDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<EscalationDTO> getEscalation(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get Escalation : {}", id);
        Optional<EscalationDTO> escalationDTO = escalationService.findOne(id);
        return ResponseUtil.wrapOrNotFound(escalationDTO);
    }
}
