package cm.gimac.des.incident.web.rest;

import cm.gimac.des.incident.service.CrisisDecisionService;
import cm.gimac.des.incident.service.dto.CrisisDecisionDTO;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link cm.gimac.des.incident.domain.CrisisDecision}.
 */
@RestController
@RequestMapping("/api/crisis-decisions")
public class CrisisDecisionResource {

    private static final Logger LOG = LoggerFactory.getLogger(CrisisDecisionResource.class);

    private final CrisisDecisionService crisisDecisionService;

    public CrisisDecisionResource(CrisisDecisionService crisisDecisionService) {
        this.crisisDecisionService = crisisDecisionService;
    }

    /**
     * {@code GET  /crisis-decisions} : get all the crisisDecisions.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of crisisDecisions in body.
     */
    @GetMapping("")
    public List<CrisisDecisionDTO> getAllCrisisDecisions(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all CrisisDecisions");
        return crisisDecisionService.findAll();
    }

    /**
     * {@code GET  /crisis-decisions/:id} : get the "id" crisisDecision.
     *
     * @param id the id of the crisisDecisionDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the crisisDecisionDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CrisisDecisionDTO> getCrisisDecision(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get CrisisDecision : {}", id);
        Optional<CrisisDecisionDTO> crisisDecisionDTO = crisisDecisionService.findOne(id);
        return ResponseUtil.wrapOrNotFound(crisisDecisionDTO);
    }
}
