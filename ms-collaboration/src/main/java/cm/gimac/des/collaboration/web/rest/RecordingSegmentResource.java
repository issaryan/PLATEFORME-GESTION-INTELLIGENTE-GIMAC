package cm.gimac.des.collaboration.web.rest;

import cm.gimac.des.collaboration.service.RecordingSegmentService;
import cm.gimac.des.collaboration.service.dto.RecordingSegmentDTO;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link cm.gimac.des.collaboration.domain.RecordingSegment}.
 */
@RestController
@RequestMapping("/api/recording-segments")
public class RecordingSegmentResource {

    private static final Logger LOG = LoggerFactory.getLogger(RecordingSegmentResource.class);

    private final RecordingSegmentService recordingSegmentService;

    public RecordingSegmentResource(RecordingSegmentService recordingSegmentService) {
        this.recordingSegmentService = recordingSegmentService;
    }

    /**
     * {@code GET  /recording-segments} : get all the Recording Segments.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Recording Segments in body.
     */
    @GetMapping("")
    public List<RecordingSegmentDTO> getAllRecordingSegments(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all RecordingSegments");
        if (eagerload) {
            return recordingSegmentService.findAllWithEagerRelationships();
        } else {
            return recordingSegmentService.findAll();
        }
    }

    /**
     * {@code GET  /recording-segments/:id} : get the "id" recordingSegment.
     *
     * @param id the id of the recordingSegmentDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the recordingSegmentDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RecordingSegmentDTO> getRecordingSegment(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get RecordingSegment : {}", id);
        Optional<RecordingSegmentDTO> recordingSegmentDTO = recordingSegmentService.findOne(id);
        return ResponseUtil.wrapOrNotFound(recordingSegmentDTO);
    }
}
