package cm.gimac.des.incident.web.rest;

import cm.gimac.des.incident.service.EngagementFreezeService;
import cm.gimac.des.incident.service.dto.EngagementFreezeDTO;
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
 * REST controller for managing {@link cm.gimac.des.incident.domain.EngagementFreeze}.
 */
@RestController
@RequestMapping("/api/engagement-freezes")
public class EngagementFreezeResource {

    private static final Logger LOG = LoggerFactory.getLogger(EngagementFreezeResource.class);

    private final EngagementFreezeService engagementFreezeService;

    public EngagementFreezeResource(EngagementFreezeService engagementFreezeService) {
        this.engagementFreezeService = engagementFreezeService;
    }

    /**
     * {@code GET  /engagement-freezes} : get all the engagementFreezes.
     *
     * @param pageable the pagination information.
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of engagementFreezes in body.
     */
    @GetMapping("")
    public ResponseEntity<List<EngagementFreezeDTO>> getAllEngagementFreezes(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get a page of EngagementFreezes");
        Page<EngagementFreezeDTO> page;
        if (eagerload) {
            page = engagementFreezeService.findAllWithEagerRelationships(pageable);
        } else {
            page = engagementFreezeService.findAll(pageable);
        }
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /engagement-freezes/:id} : get the "id" engagementFreeze.
     *
     * @param id the id of the engagementFreezeDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the engagementFreezeDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<EngagementFreezeDTO> getEngagementFreeze(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get EngagementFreeze : {}", id);
        Optional<EngagementFreezeDTO> engagementFreezeDTO = engagementFreezeService.findOne(id);
        return ResponseUtil.wrapOrNotFound(engagementFreezeDTO);
    }
}
