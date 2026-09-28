package cm.gimac.des.document.web.rest;

import cm.gimac.des.document.service.ConsultationQueryService;
import cm.gimac.des.document.service.ConsultationService;
import cm.gimac.des.document.service.criteria.ConsultationCriteria;
import cm.gimac.des.document.service.dto.ConsultationDTO;
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
 * REST controller for managing {@link cm.gimac.des.document.domain.Consultation}.
 */
@RestController
@RequestMapping("/api/consultations")
public class ConsultationResource {

    private static final Logger LOG = LoggerFactory.getLogger(ConsultationResource.class);

    private final ConsultationService consultationService;

    private final ConsultationQueryService consultationQueryService;

    public ConsultationResource(ConsultationService consultationService, ConsultationQueryService consultationQueryService) {
        this.consultationService = consultationService;
        this.consultationQueryService = consultationQueryService;
    }

    /**
     * {@code GET  /consultations} : get all the consultations.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of consultations in body.
     */
    @GetMapping("")
    public ResponseEntity<List<ConsultationDTO>> getAllConsultations(
        ConsultationCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get Consultations by criteria: {}", criteria);

        Page<ConsultationDTO> page = consultationQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /consultations/count} : count all the consultations.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countConsultations(ConsultationCriteria criteria) {
        LOG.debug("REST request to count Consultations by criteria: {}", criteria);
        return ResponseEntity.ok().body(consultationQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /consultations/:id} : get the "id" consultation.
     *
     * @param id the id of the consultationDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the consultationDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ConsultationDTO> getConsultation(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get Consultation : {}", id);
        Optional<ConsultationDTO> consultationDTO = consultationService.findOne(id);
        return ResponseUtil.wrapOrNotFound(consultationDTO);
    }
}
