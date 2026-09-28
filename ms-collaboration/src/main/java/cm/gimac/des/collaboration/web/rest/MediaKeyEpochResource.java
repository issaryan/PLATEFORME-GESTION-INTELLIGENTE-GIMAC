package cm.gimac.des.collaboration.web.rest;

import cm.gimac.des.collaboration.service.MediaKeyEpochService;
import cm.gimac.des.collaboration.service.dto.MediaKeyEpochDTO;
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
 * REST controller for managing {@link cm.gimac.des.collaboration.domain.MediaKeyEpoch}.
 */
@RestController
@RequestMapping("/api/media-key-epoches")
public class MediaKeyEpochResource {

    private static final Logger LOG = LoggerFactory.getLogger(MediaKeyEpochResource.class);

    private final MediaKeyEpochService mediaKeyEpochService;

    public MediaKeyEpochResource(MediaKeyEpochService mediaKeyEpochService) {
        this.mediaKeyEpochService = mediaKeyEpochService;
    }

    /**
     * {@code GET  /media-key-epoches} : get all the mediaKeyEpoches.
     *
     * @param pageable the pagination information.
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of mediaKeyEpoches in body.
     */
    @GetMapping("")
    public ResponseEntity<List<MediaKeyEpochDTO>> getAllMediaKeyEpoches(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get a page of MediaKeyEpoches");
        Page<MediaKeyEpochDTO> page;
        if (eagerload) {
            page = mediaKeyEpochService.findAllWithEagerRelationships(pageable);
        } else {
            page = mediaKeyEpochService.findAll(pageable);
        }
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /media-key-epoches/:id} : get the "id" mediaKeyEpoch.
     *
     * @param id the id of the mediaKeyEpochDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the mediaKeyEpochDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<MediaKeyEpochDTO> getMediaKeyEpoch(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get MediaKeyEpoch : {}", id);
        Optional<MediaKeyEpochDTO> mediaKeyEpochDTO = mediaKeyEpochService.findOne(id);
        return ResponseUtil.wrapOrNotFound(mediaKeyEpochDTO);
    }
}
