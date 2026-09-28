package cm.gimac.des.certification.web.rest;

import cm.gimac.des.certification.service.CertificationAttestationService;
import cm.gimac.des.certification.service.dto.CertificationAttestationDTO;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link cm.gimac.des.certification.domain.CertificationAttestation}.
 */
@RestController
@RequestMapping("/api/certification-attestations")
public class CertificationAttestationResource {

    private static final Logger LOG = LoggerFactory.getLogger(CertificationAttestationResource.class);

    private final CertificationAttestationService certificationAttestationService;

    public CertificationAttestationResource(CertificationAttestationService certificationAttestationService) {
        this.certificationAttestationService = certificationAttestationService;
    }

    /**
     * {@code GET  /certification-attestations} : get all the certificationAttestations.
     *
     * @param filter the filter of the request.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of certificationAttestations in body.
     */
    @GetMapping("")
    public List<CertificationAttestationDTO> getAllCertificationAttestations(
        @RequestParam(name = "filter", required = false) String filter
    ) {
        if ("commissioningfile-is-null".equals(filter)) {
            LOG.debug("REST request to get all CertificationAttestations where commissioningFile is null");
            return certificationAttestationService.findAllWhereCommissioningFileIsNull();
        }
        LOG.debug("REST request to get all CertificationAttestations");
        return certificationAttestationService.findAll();
    }

    /**
     * {@code GET  /certification-attestations/:id} : get the "id" certificationAttestation.
     *
     * @param id the id of the certificationAttestationDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the certificationAttestationDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CertificationAttestationDTO> getCertificationAttestation(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get CertificationAttestation : {}", id);
        Optional<CertificationAttestationDTO> certificationAttestationDTO = certificationAttestationService.findOne(id);
        return ResponseUtil.wrapOrNotFound(certificationAttestationDTO);
    }
}
