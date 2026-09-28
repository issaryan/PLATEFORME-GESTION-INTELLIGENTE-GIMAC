package cm.gimac.des.rapprochement.web.rest;

import cm.gimac.des.rapprochement.service.RegularizationAttestationService;
import cm.gimac.des.rapprochement.service.dto.RegularizationAttestationDTO;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link cm.gimac.des.rapprochement.domain.RegularizationAttestation}.
 */
@RestController
@RequestMapping("/api/regularization-attestations")
public class RegularizationAttestationResource {

    private static final Logger LOG = LoggerFactory.getLogger(RegularizationAttestationResource.class);

    private final RegularizationAttestationService regularizationAttestationService;

    public RegularizationAttestationResource(RegularizationAttestationService regularizationAttestationService) {
        this.regularizationAttestationService = regularizationAttestationService;
    }

    /**
     * {@code GET  /regularization-attestations} : get all the regularizationAttestations.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of regularizationAttestations in body.
     */
    @GetMapping("")
    public List<RegularizationAttestationDTO> getAllRegularizationAttestations(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all RegularizationAttestations");
        return regularizationAttestationService.findAll();
    }

    /**
     * {@code GET  /regularization-attestations/:id} : get the "id" regularizationAttestation.
     *
     * @param id the id of the regularizationAttestationDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the regularizationAttestationDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RegularizationAttestationDTO> getRegularizationAttestation(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get RegularizationAttestation : {}", id);
        Optional<RegularizationAttestationDTO> regularizationAttestationDTO = regularizationAttestationService.findOne(id);
        return ResponseUtil.wrapOrNotFound(regularizationAttestationDTO);
    }
}
