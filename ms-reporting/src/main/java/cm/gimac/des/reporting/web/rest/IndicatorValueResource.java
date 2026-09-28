package cm.gimac.des.reporting.web.rest;

import cm.gimac.des.reporting.service.IndicatorValueQueryService;
import cm.gimac.des.reporting.service.IndicatorValueService;
import cm.gimac.des.reporting.service.criteria.IndicatorValueCriteria;
import cm.gimac.des.reporting.service.dto.IndicatorValueDTO;
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
 * REST controller for managing {@link cm.gimac.des.reporting.domain.IndicatorValue}.
 */
@RestController
@RequestMapping("/api/indicator-values")
public class IndicatorValueResource {

    private static final Logger LOG = LoggerFactory.getLogger(IndicatorValueResource.class);

    private final IndicatorValueService indicatorValueService;

    private final IndicatorValueQueryService indicatorValueQueryService;

    public IndicatorValueResource(IndicatorValueService indicatorValueService, IndicatorValueQueryService indicatorValueQueryService) {
        this.indicatorValueService = indicatorValueService;
        this.indicatorValueQueryService = indicatorValueQueryService;
    }

    /**
     * {@code GET  /indicator-values} : get all the indicatorValues.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of indicatorValues in body.
     */
    @GetMapping("")
    public ResponseEntity<List<IndicatorValueDTO>> getAllIndicatorValues(
        IndicatorValueCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get IndicatorValues by criteria: {}", criteria);

        Page<IndicatorValueDTO> page = indicatorValueQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /indicator-values/count} : count all the indicatorValues.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countIndicatorValues(IndicatorValueCriteria criteria) {
        LOG.debug("REST request to count IndicatorValues by criteria: {}", criteria);
        return ResponseEntity.ok().body(indicatorValueQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /indicator-values/:id} : get the "id" indicatorValue.
     *
     * @param id the id of the indicatorValueDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the indicatorValueDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<IndicatorValueDTO> getIndicatorValue(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get IndicatorValue : {}", id);
        Optional<IndicatorValueDTO> indicatorValueDTO = indicatorValueService.findOne(id);
        return ResponseUtil.wrapOrNotFound(indicatorValueDTO);
    }
}
