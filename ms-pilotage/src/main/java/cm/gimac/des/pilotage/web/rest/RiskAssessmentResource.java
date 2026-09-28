package cm.gimac.des.pilotage.web.rest;

import cm.gimac.des.pilotage.service.RiskAssessmentQueryService;
import cm.gimac.des.pilotage.service.RiskAssessmentService;
import cm.gimac.des.pilotage.service.criteria.RiskAssessmentCriteria;
import cm.gimac.des.pilotage.service.dto.RiskAssessmentDTO;
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
 * REST controller for managing {@link cm.gimac.des.pilotage.domain.RiskAssessment}.
 */
@RestController
@RequestMapping("/api/risk-assessments")
public class RiskAssessmentResource {

    private static final Logger LOG = LoggerFactory.getLogger(RiskAssessmentResource.class);

    private final RiskAssessmentService riskAssessmentService;

    private final RiskAssessmentQueryService riskAssessmentQueryService;

    public RiskAssessmentResource(RiskAssessmentService riskAssessmentService, RiskAssessmentQueryService riskAssessmentQueryService) {
        this.riskAssessmentService = riskAssessmentService;
        this.riskAssessmentQueryService = riskAssessmentQueryService;
    }

    /**
     * {@code GET  /risk-assessments} : get all the riskAssessments.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of riskAssessments in body.
     */
    @GetMapping("")
    public ResponseEntity<List<RiskAssessmentDTO>> getAllRiskAssessments(
        RiskAssessmentCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get RiskAssessments by criteria: {}", criteria);

        Page<RiskAssessmentDTO> page = riskAssessmentQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /risk-assessments/count} : count all the riskAssessments.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countRiskAssessments(RiskAssessmentCriteria criteria) {
        LOG.debug("REST request to count RiskAssessments by criteria: {}", criteria);
        return ResponseEntity.ok().body(riskAssessmentQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /risk-assessments/:id} : get the "id" riskAssessment.
     *
     * @param id the id of the riskAssessmentDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the riskAssessmentDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RiskAssessmentDTO> getRiskAssessment(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get RiskAssessment : {}", id);
        Optional<RiskAssessmentDTO> riskAssessmentDTO = riskAssessmentService.findOne(id);
        return ResponseUtil.wrapOrNotFound(riskAssessmentDTO);
    }
}
