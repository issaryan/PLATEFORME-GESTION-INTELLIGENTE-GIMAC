package cm.gimac.des.certification.web.rest;

import cm.gimac.des.certification.service.CommissioningFileQueryService;
import cm.gimac.des.certification.service.CommissioningFileService;
import cm.gimac.des.certification.service.criteria.CommissioningFileCriteria;
import cm.gimac.des.certification.service.dto.CommissioningFileDTO;
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
 * REST controller for managing {@link cm.gimac.des.certification.domain.CommissioningFile}.
 */
@RestController
@RequestMapping("/api/commissioning-files")
public class CommissioningFileResource {

    private static final Logger LOG = LoggerFactory.getLogger(CommissioningFileResource.class);

    private final CommissioningFileService commissioningFileService;

    private final CommissioningFileQueryService commissioningFileQueryService;

    public CommissioningFileResource(
        CommissioningFileService commissioningFileService,
        CommissioningFileQueryService commissioningFileQueryService
    ) {
        this.commissioningFileService = commissioningFileService;
        this.commissioningFileQueryService = commissioningFileQueryService;
    }

    /**
     * {@code GET  /commissioning-files} : get all the commissioningFiles.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of commissioningFiles in body.
     */
    @GetMapping("")
    public ResponseEntity<List<CommissioningFileDTO>> getAllCommissioningFiles(
        CommissioningFileCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get CommissioningFiles by criteria: {}", criteria);

        Page<CommissioningFileDTO> page = commissioningFileQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /commissioning-files/count} : count all the commissioningFiles.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countCommissioningFiles(CommissioningFileCriteria criteria) {
        LOG.debug("REST request to count CommissioningFiles by criteria: {}", criteria);
        return ResponseEntity.ok().body(commissioningFileQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /commissioning-files/:id} : get the "id" commissioningFile.
     *
     * @param id the id of the commissioningFileDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the commissioningFileDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CommissioningFileDTO> getCommissioningFile(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get CommissioningFile : {}", id);
        Optional<CommissioningFileDTO> commissioningFileDTO = commissioningFileService.findOne(id);
        return ResponseUtil.wrapOrNotFound(commissioningFileDTO);
    }
}
