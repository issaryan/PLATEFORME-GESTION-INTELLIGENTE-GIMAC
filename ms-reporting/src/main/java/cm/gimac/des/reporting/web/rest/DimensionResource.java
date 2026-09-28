package cm.gimac.des.reporting.web.rest;

import cm.gimac.des.reporting.service.DimensionQueryService;
import cm.gimac.des.reporting.service.DimensionService;
import cm.gimac.des.reporting.service.criteria.DimensionCriteria;
import cm.gimac.des.reporting.service.dto.DimensionDTO;
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
 * REST controller for managing {@link cm.gimac.des.reporting.domain.Dimension}.
 */
@RestController
@RequestMapping("/api/dimensions")
public class DimensionResource {

    private static final Logger LOG = LoggerFactory.getLogger(DimensionResource.class);

    private final DimensionService dimensionService;

    private final DimensionQueryService dimensionQueryService;

    public DimensionResource(DimensionService dimensionService, DimensionQueryService dimensionQueryService) {
        this.dimensionService = dimensionService;
        this.dimensionQueryService = dimensionQueryService;
    }

    /**
     * {@code GET  /dimensions} : get all the dimensions.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of dimensions in body.
     */
    @GetMapping("")
    public ResponseEntity<List<DimensionDTO>> getAllDimensions(
        DimensionCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get Dimensions by criteria: {}", criteria);

        Page<DimensionDTO> page = dimensionQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /dimensions/count} : count all the dimensions.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countDimensions(DimensionCriteria criteria) {
        LOG.debug("REST request to count Dimensions by criteria: {}", criteria);
        return ResponseEntity.ok().body(dimensionQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /dimensions/:id} : get the "id" dimension.
     *
     * @param id the id of the dimensionDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the dimensionDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DimensionDTO> getDimension(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get Dimension : {}", id);
        Optional<DimensionDTO> dimensionDTO = dimensionService.findOne(id);
        return ResponseUtil.wrapOrNotFound(dimensionDTO);
    }
}
