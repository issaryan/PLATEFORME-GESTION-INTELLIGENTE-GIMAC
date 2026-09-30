package cm.gimac.des.collaboration.web.rest;

import cm.gimac.des.collaboration.service.RecordingConsentService;
import cm.gimac.des.collaboration.service.dto.RecordingConsentDTO;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link cm.gimac.des.collaboration.domain.RecordingConsent}.
 */
@RestController
@RequestMapping("/api/recording-consents")
public class RecordingConsentResource {

    private static final Logger LOG = LoggerFactory.getLogger(RecordingConsentResource.class);

    private final RecordingConsentService recordingConsentService;

    public RecordingConsentResource(RecordingConsentService recordingConsentService) {
        this.recordingConsentService = recordingConsentService;
    }

    /**
     * {@code GET  /recording-consents} : get all the Recording Consents.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Recording Consents in body.
     */
    @GetMapping("")
    public List<RecordingConsentDTO> getAllRecordingConsents(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all RecordingConsents");
        if (eagerload) {
            return recordingConsentService.findAllWithEagerRelationships();
        } else {
            return recordingConsentService.findAll();
        }
    }

    /**
     * {@code GET  /recording-consents/:id} : get the "id" recordingConsent.
     *
     * @param id the id of the recordingConsentDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the recordingConsentDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RecordingConsentDTO> getRecordingConsent(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get RecordingConsent : {}", id);
        Optional<RecordingConsentDTO> recordingConsentDTO = recordingConsentService.findOne(id);
        return ResponseUtil.wrapOrNotFound(recordingConsentDTO);
    }
}
