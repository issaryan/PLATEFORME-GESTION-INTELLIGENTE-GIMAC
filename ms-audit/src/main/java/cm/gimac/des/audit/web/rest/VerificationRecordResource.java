package cm.gimac.des.audit.web.rest;

import cm.gimac.des.audit.service.VerificationRecordQueryService;
import cm.gimac.des.audit.service.VerificationRecordService;
import cm.gimac.des.audit.service.criteria.VerificationRecordCriteria;
import cm.gimac.des.audit.service.dto.VerificationRecordDTO;
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
 * REST controller for managing {@link cm.gimac.des.audit.domain.VerificationRecord}.
 */
@RestController
@RequestMapping("/api/verification-records")
public class VerificationRecordResource {

    private static final Logger LOG = LoggerFactory.getLogger(VerificationRecordResource.class);

    private final VerificationRecordService verificationRecordService;

    private final VerificationRecordQueryService verificationRecordQueryService;

    public VerificationRecordResource(
        VerificationRecordService verificationRecordService,
        VerificationRecordQueryService verificationRecordQueryService
    ) {
        this.verificationRecordService = verificationRecordService;
        this.verificationRecordQueryService = verificationRecordQueryService;
    }

    /**
     * {@code GET  /verification-records} : get all the Verification Records.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Verification Records in body.
     */
    @GetMapping("")
    public ResponseEntity<List<VerificationRecordDTO>> getAllVerificationRecords(
        VerificationRecordCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get VerificationRecords by criteria: {}", criteria);

        Page<VerificationRecordDTO> page = verificationRecordQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /verification-records/count} : count all the verificationRecords.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countVerificationRecords(VerificationRecordCriteria criteria) {
        LOG.debug("REST request to count VerificationRecords by criteria: {}", criteria);
        return ResponseEntity.ok().body(verificationRecordQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /verification-records/:id} : get the "id" verificationRecord.
     *
     * @param id the id of the verificationRecordDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the verificationRecordDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<VerificationRecordDTO> getVerificationRecord(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get VerificationRecord : {}", id);
        Optional<VerificationRecordDTO> verificationRecordDTO = verificationRecordService.findOne(id);
        return ResponseUtil.wrapOrNotFound(verificationRecordDTO);
    }
}
