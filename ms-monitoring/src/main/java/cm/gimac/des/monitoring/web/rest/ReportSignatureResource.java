package cm.gimac.des.monitoring.web.rest;

import cm.gimac.des.monitoring.service.ReportSignatureService;
import cm.gimac.des.monitoring.service.dto.ReportSignatureDTO;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link cm.gimac.des.monitoring.domain.ReportSignature}.
 */
@RestController
@RequestMapping("/api/report-signatures")
public class ReportSignatureResource {

    private static final Logger LOG = LoggerFactory.getLogger(ReportSignatureResource.class);

    private final ReportSignatureService reportSignatureService;

    public ReportSignatureResource(ReportSignatureService reportSignatureService) {
        this.reportSignatureService = reportSignatureService;
    }

    /**
     * {@code GET  /report-signatures} : get all the reportSignatures.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of reportSignatures in body.
     */
    @GetMapping("")
    public List<ReportSignatureDTO> getAllReportSignatures(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all ReportSignatures");
        return reportSignatureService.findAll();
    }

    /**
     * {@code GET  /report-signatures/:id} : get the "id" reportSignature.
     *
     * @param id the id of the reportSignatureDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the reportSignatureDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReportSignatureDTO> getReportSignature(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get ReportSignature : {}", id);
        Optional<ReportSignatureDTO> reportSignatureDTO = reportSignatureService.findOne(id);
        return ResponseUtil.wrapOrNotFound(reportSignatureDTO);
    }
}
