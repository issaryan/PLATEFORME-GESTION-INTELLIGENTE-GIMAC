package cm.gimac.des.audit.web.rest;

import cm.gimac.des.audit.service.ConsistencyProofService;
import cm.gimac.des.audit.service.dto.ConsistencyProofDTO;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link cm.gimac.des.audit.domain.ConsistencyProof}.
 */
@RestController
@RequestMapping("/api/consistency-proofs")
public class ConsistencyProofResource {

    private static final Logger LOG = LoggerFactory.getLogger(ConsistencyProofResource.class);

    private final ConsistencyProofService consistencyProofService;

    public ConsistencyProofResource(ConsistencyProofService consistencyProofService) {
        this.consistencyProofService = consistencyProofService;
    }

    /**
     * {@code GET  /consistency-proofs} : get all the Consistency Proofs.
     *
     * @param filter the filter of the request.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Consistency Proofs in body.
     */
    @GetMapping("")
    public List<ConsistencyProofDTO> getAllConsistencyProofs(@RequestParam(name = "filter", required = false) String filter) {
        if ("periodroot-is-null".equals(filter)) {
            LOG.debug("REST request to get all ConsistencyProofs where periodRoot is null");
            return consistencyProofService.findAllWherePeriodRootIsNull();
        }
        LOG.debug("REST request to get all ConsistencyProofs");
        return consistencyProofService.findAll();
    }

    /**
     * {@code GET  /consistency-proofs/:id} : get the "id" consistencyProof.
     *
     * @param id the id of the consistencyProofDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the consistencyProofDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ConsistencyProofDTO> getConsistencyProof(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get ConsistencyProof : {}", id);
        Optional<ConsistencyProofDTO> consistencyProofDTO = consistencyProofService.findOne(id);
        return ResponseUtil.wrapOrNotFound(consistencyProofDTO);
    }
}
