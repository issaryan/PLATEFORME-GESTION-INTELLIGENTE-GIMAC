package cm.gimac.des.audit.web.rest;

import cm.gimac.des.audit.service.InclusionProofService;
import cm.gimac.des.audit.service.dto.InclusionProofDTO;
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
 * REST controller for managing {@link cm.gimac.des.audit.domain.InclusionProof}.
 */
@RestController
@RequestMapping("/api/inclusion-proofs")
public class InclusionProofResource {

    private static final Logger LOG = LoggerFactory.getLogger(InclusionProofResource.class);

    private final InclusionProofService inclusionProofService;

    public InclusionProofResource(InclusionProofService inclusionProofService) {
        this.inclusionProofService = inclusionProofService;
    }

    /**
     * {@code GET  /inclusion-proofs} : get all the Inclusion Proofs.
     *
     * @param pageable the pagination information.
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Inclusion Proofs in body.
     */
    @GetMapping("")
    public ResponseEntity<List<InclusionProofDTO>> getAllInclusionProofs(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get a page of InclusionProofs");
        Page<InclusionProofDTO> page;
        if (eagerload) {
            page = inclusionProofService.findAllWithEagerRelationships(pageable);
        } else {
            page = inclusionProofService.findAll(pageable);
        }
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /inclusion-proofs/:id} : get the "id" inclusionProof.
     *
     * @param id the id of the inclusionProofDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the inclusionProofDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<InclusionProofDTO> getInclusionProof(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get InclusionProof : {}", id);
        Optional<InclusionProofDTO> inclusionProofDTO = inclusionProofService.findOne(id);
        return ResponseUtil.wrapOrNotFound(inclusionProofDTO);
    }
}
