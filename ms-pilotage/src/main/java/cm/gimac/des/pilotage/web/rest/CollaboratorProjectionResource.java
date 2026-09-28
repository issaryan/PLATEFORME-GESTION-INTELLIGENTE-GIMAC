package cm.gimac.des.pilotage.web.rest;

import cm.gimac.des.pilotage.service.CollaboratorProjectionQueryService;
import cm.gimac.des.pilotage.service.CollaboratorProjectionService;
import cm.gimac.des.pilotage.service.criteria.CollaboratorProjectionCriteria;
import cm.gimac.des.pilotage.service.dto.CollaboratorProjectionDTO;
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
 * REST controller for managing {@link cm.gimac.des.pilotage.domain.CollaboratorProjection}.
 */
@RestController
@RequestMapping("/api/collaborator-projections")
public class CollaboratorProjectionResource {

    private static final Logger LOG = LoggerFactory.getLogger(CollaboratorProjectionResource.class);

    private final CollaboratorProjectionService collaboratorProjectionService;

    private final CollaboratorProjectionQueryService collaboratorProjectionQueryService;

    public CollaboratorProjectionResource(
        CollaboratorProjectionService collaboratorProjectionService,
        CollaboratorProjectionQueryService collaboratorProjectionQueryService
    ) {
        this.collaboratorProjectionService = collaboratorProjectionService;
        this.collaboratorProjectionQueryService = collaboratorProjectionQueryService;
    }

    /**
     * {@code GET  /collaborator-projections} : get all the collaboratorProjections.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of collaboratorProjections in body.
     */
    @GetMapping("")
    public ResponseEntity<List<CollaboratorProjectionDTO>> getAllCollaboratorProjections(
        CollaboratorProjectionCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get CollaboratorProjections by criteria: {}", criteria);

        Page<CollaboratorProjectionDTO> page = collaboratorProjectionQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /collaborator-projections/count} : count all the collaboratorProjections.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countCollaboratorProjections(CollaboratorProjectionCriteria criteria) {
        LOG.debug("REST request to count CollaboratorProjections by criteria: {}", criteria);
        return ResponseEntity.ok().body(collaboratorProjectionQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /collaborator-projections/:id} : get the "id" collaboratorProjection.
     *
     * @param id the id of the collaboratorProjectionDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the collaboratorProjectionDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CollaboratorProjectionDTO> getCollaboratorProjection(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get CollaboratorProjection : {}", id);
        Optional<CollaboratorProjectionDTO> collaboratorProjectionDTO = collaboratorProjectionService.findOne(id);
        return ResponseUtil.wrapOrNotFound(collaboratorProjectionDTO);
    }
}
