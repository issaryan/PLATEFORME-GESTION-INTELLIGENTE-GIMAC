package cm.gimac.des.certification.web.rest;

import cm.gimac.des.certification.service.CoverageSnapshotQueryService;
import cm.gimac.des.certification.service.CoverageSnapshotService;
import cm.gimac.des.certification.service.criteria.CoverageSnapshotCriteria;
import cm.gimac.des.certification.service.dto.CoverageSnapshotDTO;
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
 * REST controller for managing {@link cm.gimac.des.certification.domain.CoverageSnapshot}.
 */
@RestController
@RequestMapping("/api/coverage-snapshots")
public class CoverageSnapshotResource {

    private static final Logger LOG = LoggerFactory.getLogger(CoverageSnapshotResource.class);

    private final CoverageSnapshotService coverageSnapshotService;

    private final CoverageSnapshotQueryService coverageSnapshotQueryService;

    public CoverageSnapshotResource(
        CoverageSnapshotService coverageSnapshotService,
        CoverageSnapshotQueryService coverageSnapshotQueryService
    ) {
        this.coverageSnapshotService = coverageSnapshotService;
        this.coverageSnapshotQueryService = coverageSnapshotQueryService;
    }

    /**
     * {@code GET  /coverage-snapshots} : get all the coverageSnapshots.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of coverageSnapshots in body.
     */
    @GetMapping("")
    public ResponseEntity<List<CoverageSnapshotDTO>> getAllCoverageSnapshots(
        CoverageSnapshotCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get CoverageSnapshots by criteria: {}", criteria);

        Page<CoverageSnapshotDTO> page = coverageSnapshotQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /coverage-snapshots/count} : count all the coverageSnapshots.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countCoverageSnapshots(CoverageSnapshotCriteria criteria) {
        LOG.debug("REST request to count CoverageSnapshots by criteria: {}", criteria);
        return ResponseEntity.ok().body(coverageSnapshotQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /coverage-snapshots/:id} : get the "id" coverageSnapshot.
     *
     * @param id the id of the coverageSnapshotDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the coverageSnapshotDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CoverageSnapshotDTO> getCoverageSnapshot(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get CoverageSnapshot : {}", id);
        Optional<CoverageSnapshotDTO> coverageSnapshotDTO = coverageSnapshotService.findOne(id);
        return ResponseUtil.wrapOrNotFound(coverageSnapshotDTO);
    }
}
