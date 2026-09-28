package cm.gimac.des.certification.web.rest;

import cm.gimac.des.certification.service.ClosedOperationSnapshotQueryService;
import cm.gimac.des.certification.service.ClosedOperationSnapshotService;
import cm.gimac.des.certification.service.criteria.ClosedOperationSnapshotCriteria;
import cm.gimac.des.certification.service.dto.ClosedOperationSnapshotDTO;
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
 * REST controller for managing {@link cm.gimac.des.certification.domain.ClosedOperationSnapshot}.
 */
@RestController
@RequestMapping("/api/closed-operation-snapshots")
public class ClosedOperationSnapshotResource {

    private static final Logger LOG = LoggerFactory.getLogger(ClosedOperationSnapshotResource.class);

    private final ClosedOperationSnapshotService closedOperationSnapshotService;

    private final ClosedOperationSnapshotQueryService closedOperationSnapshotQueryService;

    public ClosedOperationSnapshotResource(
        ClosedOperationSnapshotService closedOperationSnapshotService,
        ClosedOperationSnapshotQueryService closedOperationSnapshotQueryService
    ) {
        this.closedOperationSnapshotService = closedOperationSnapshotService;
        this.closedOperationSnapshotQueryService = closedOperationSnapshotQueryService;
    }

    /**
     * {@code GET  /closed-operation-snapshots} : get all the closedOperationSnapshots.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of closedOperationSnapshots in body.
     */
    @GetMapping("")
    public ResponseEntity<List<ClosedOperationSnapshotDTO>> getAllClosedOperationSnapshots(
        ClosedOperationSnapshotCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get ClosedOperationSnapshots by criteria: {}", criteria);

        Page<ClosedOperationSnapshotDTO> page = closedOperationSnapshotQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /closed-operation-snapshots/count} : count all the closedOperationSnapshots.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countClosedOperationSnapshots(ClosedOperationSnapshotCriteria criteria) {
        LOG.debug("REST request to count ClosedOperationSnapshots by criteria: {}", criteria);
        return ResponseEntity.ok().body(closedOperationSnapshotQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /closed-operation-snapshots/:id} : get the "id" closedOperationSnapshot.
     *
     * @param id the id of the closedOperationSnapshotDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the closedOperationSnapshotDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ClosedOperationSnapshotDTO> getClosedOperationSnapshot(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get ClosedOperationSnapshot : {}", id);
        Optional<ClosedOperationSnapshotDTO> closedOperationSnapshotDTO = closedOperationSnapshotService.findOne(id);
        return ResponseUtil.wrapOrNotFound(closedOperationSnapshotDTO);
    }
}
