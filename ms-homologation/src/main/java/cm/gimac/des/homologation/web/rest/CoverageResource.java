package cm.gimac.des.homologation.web.rest;

import cm.gimac.des.homologation.service.CoverageQueryService;
import cm.gimac.des.homologation.service.CoverageService;
import cm.gimac.des.homologation.service.criteria.CoverageCriteria;
import cm.gimac.des.homologation.service.dto.CoverageDTO;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link cm.gimac.des.homologation.domain.Coverage}.
 */
@RestController
@RequestMapping("/api/coverages")
public class CoverageResource {

    private static final Logger LOG = LoggerFactory.getLogger(CoverageResource.class);

    private final CoverageService coverageService;

    private final CoverageQueryService coverageQueryService;

    public CoverageResource(CoverageService coverageService, CoverageQueryService coverageQueryService) {
        this.coverageService = coverageService;
        this.coverageQueryService = coverageQueryService;
    }

    /**
     * {@code GET  /coverages} : get all the coverages.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of coverages in body.
     */
    @GetMapping("")
    public ResponseEntity<List<CoverageDTO>> getAllCoverages(CoverageCriteria criteria) {
        LOG.debug("REST request to get Coverages by criteria: {}", criteria);

        List<CoverageDTO> entityList = coverageQueryService.findByCriteria(criteria);
        return ResponseEntity.ok().body(entityList);
    }

    /**
     * {@code GET  /coverages/count} : count all the coverages.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countCoverages(CoverageCriteria criteria) {
        LOG.debug("REST request to count Coverages by criteria: {}", criteria);
        return ResponseEntity.ok().body(coverageQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /coverages/:id} : get the "id" coverage.
     *
     * @param id the id of the coverageDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the coverageDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CoverageDTO> getCoverage(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get Coverage : {}", id);
        Optional<CoverageDTO> coverageDTO = coverageService.findOne(id);
        return ResponseUtil.wrapOrNotFound(coverageDTO);
    }
}
