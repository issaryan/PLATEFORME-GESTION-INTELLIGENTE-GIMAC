package cm.gimac.des.pilotage.web.rest;

import cm.gimac.des.pilotage.service.AbsenceProjectionQueryService;
import cm.gimac.des.pilotage.service.AbsenceProjectionService;
import cm.gimac.des.pilotage.service.criteria.AbsenceProjectionCriteria;
import cm.gimac.des.pilotage.service.dto.AbsenceProjectionDTO;
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
 * REST controller for managing {@link cm.gimac.des.pilotage.domain.AbsenceProjection}.
 */
@RestController
@RequestMapping("/api/absence-projections")
public class AbsenceProjectionResource {

    private static final Logger LOG = LoggerFactory.getLogger(AbsenceProjectionResource.class);

    private final AbsenceProjectionService absenceProjectionService;

    private final AbsenceProjectionQueryService absenceProjectionQueryService;

    public AbsenceProjectionResource(
        AbsenceProjectionService absenceProjectionService,
        AbsenceProjectionQueryService absenceProjectionQueryService
    ) {
        this.absenceProjectionService = absenceProjectionService;
        this.absenceProjectionQueryService = absenceProjectionQueryService;
    }

    /**
     * {@code GET  /absence-projections} : get all the absenceProjections.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of absenceProjections in body.
     */
    @GetMapping("")
    public ResponseEntity<List<AbsenceProjectionDTO>> getAllAbsenceProjections(
        AbsenceProjectionCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get AbsenceProjections by criteria: {}", criteria);

        Page<AbsenceProjectionDTO> page = absenceProjectionQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /absence-projections/count} : count all the absenceProjections.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countAbsenceProjections(AbsenceProjectionCriteria criteria) {
        LOG.debug("REST request to count AbsenceProjections by criteria: {}", criteria);
        return ResponseEntity.ok().body(absenceProjectionQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /absence-projections/:id} : get the "id" absenceProjection.
     *
     * @param id the id of the absenceProjectionDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the absenceProjectionDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AbsenceProjectionDTO> getAbsenceProjection(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get AbsenceProjection : {}", id);
        Optional<AbsenceProjectionDTO> absenceProjectionDTO = absenceProjectionService.findOne(id);
        return ResponseUtil.wrapOrNotFound(absenceProjectionDTO);
    }
}
