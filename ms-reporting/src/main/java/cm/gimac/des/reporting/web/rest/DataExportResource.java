package cm.gimac.des.reporting.web.rest;

import cm.gimac.des.reporting.service.DataExportQueryService;
import cm.gimac.des.reporting.service.DataExportService;
import cm.gimac.des.reporting.service.criteria.DataExportCriteria;
import cm.gimac.des.reporting.service.dto.DataExportDTO;
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
 * REST controller for managing {@link cm.gimac.des.reporting.domain.DataExport}.
 */
@RestController
@RequestMapping("/api/data-exports")
public class DataExportResource {

    private static final Logger LOG = LoggerFactory.getLogger(DataExportResource.class);

    private final DataExportService dataExportService;

    private final DataExportQueryService dataExportQueryService;

    public DataExportResource(DataExportService dataExportService, DataExportQueryService dataExportQueryService) {
        this.dataExportService = dataExportService;
        this.dataExportQueryService = dataExportQueryService;
    }

    /**
     * {@code GET  /data-exports} : get all the dataExports.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of dataExports in body.
     */
    @GetMapping("")
    public ResponseEntity<List<DataExportDTO>> getAllDataExports(
        DataExportCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get DataExports by criteria: {}", criteria);

        Page<DataExportDTO> page = dataExportQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /data-exports/count} : count all the dataExports.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countDataExports(DataExportCriteria criteria) {
        LOG.debug("REST request to count DataExports by criteria: {}", criteria);
        return ResponseEntity.ok().body(dataExportQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /data-exports/:id} : get the "id" dataExport.
     *
     * @param id the id of the dataExportDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the dataExportDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DataExportDTO> getDataExport(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get DataExport : {}", id);
        Optional<DataExportDTO> dataExportDTO = dataExportService.findOne(id);
        return ResponseUtil.wrapOrNotFound(dataExportDTO);
    }
}
