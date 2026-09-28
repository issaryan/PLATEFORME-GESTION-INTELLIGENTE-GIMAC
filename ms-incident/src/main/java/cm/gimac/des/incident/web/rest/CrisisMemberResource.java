package cm.gimac.des.incident.web.rest;

import cm.gimac.des.incident.service.CrisisMemberService;
import cm.gimac.des.incident.service.dto.CrisisMemberDTO;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link cm.gimac.des.incident.domain.CrisisMember}.
 */
@RestController
@RequestMapping("/api/crisis-members")
public class CrisisMemberResource {

    private static final Logger LOG = LoggerFactory.getLogger(CrisisMemberResource.class);

    private final CrisisMemberService crisisMemberService;

    public CrisisMemberResource(CrisisMemberService crisisMemberService) {
        this.crisisMemberService = crisisMemberService;
    }

    /**
     * {@code GET  /crisis-members} : get all the crisisMembers.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of crisisMembers in body.
     */
    @GetMapping("")
    public List<CrisisMemberDTO> getAllCrisisMembers(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all CrisisMembers");
        return crisisMemberService.findAll();
    }

    /**
     * {@code GET  /crisis-members/:id} : get the "id" crisisMember.
     *
     * @param id the id of the crisisMemberDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the crisisMemberDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CrisisMemberDTO> getCrisisMember(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get CrisisMember : {}", id);
        Optional<CrisisMemberDTO> crisisMemberDTO = crisisMemberService.findOne(id);
        return ResponseUtil.wrapOrNotFound(crisisMemberDTO);
    }
}
