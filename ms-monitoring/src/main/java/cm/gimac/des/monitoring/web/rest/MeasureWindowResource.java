package cm.gimac.des.monitoring.web.rest;

import cm.gimac.des.monitoring.service.MeasureWindowQueryService;
import cm.gimac.des.monitoring.service.MeasureWindowService;
import cm.gimac.des.monitoring.service.criteria.MeasureWindowCriteria;
import cm.gimac.des.monitoring.service.dto.MeasureWindowDTO;
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
 * REST controller for managing {@link cm.gimac.des.monitoring.domain.MeasureWindow}.
 */
@RestController
@RequestMapping("/api/measure-windows")
public class MeasureWindowResource {

    private static final Logger LOG = LoggerFactory.getLogger(MeasureWindowResource.class);

    private final MeasureWindowService measureWindowService;

    private final MeasureWindowQueryService measureWindowQueryService;

    public MeasureWindowResource(MeasureWindowService measureWindowService, MeasureWindowQueryService measureWindowQueryService) {
        this.measureWindowService = measureWindowService;
        this.measureWindowQueryService = measureWindowQueryService;
    }

    /**
     * {@code GET  /measure-windows} : get all the measureWindows.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of measureWindows in body.
     */
    @GetMapping("")
    public ResponseEntity<List<MeasureWindowDTO>> getAllMeasureWindows(
        MeasureWindowCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get MeasureWindows by criteria: {}", criteria);

        Page<MeasureWindowDTO> page = measureWindowQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /measure-windows/count} : count all the measureWindows.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countMeasureWindows(MeasureWindowCriteria criteria) {
        LOG.debug("REST request to count MeasureWindows by criteria: {}", criteria);
        return ResponseEntity.ok().body(measureWindowQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /measure-windows/:id} : get the "id" measureWindow.
     *
     * @param id the id of the measureWindowDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the measureWindowDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<MeasureWindowDTO> getMeasureWindow(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get MeasureWindow : {}", id);
        Optional<MeasureWindowDTO> measureWindowDTO = measureWindowService.findOne(id);
        return ResponseUtil.wrapOrNotFound(measureWindowDTO);
    }
}
