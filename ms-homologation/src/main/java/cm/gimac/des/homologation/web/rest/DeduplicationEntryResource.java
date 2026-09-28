package cm.gimac.des.homologation.web.rest;

import cm.gimac.des.homologation.service.DeduplicationEntryService;
import cm.gimac.des.homologation.service.dto.DeduplicationEntryDTO;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link cm.gimac.des.homologation.domain.DeduplicationEntry}.
 */
@RestController
@RequestMapping("/api/deduplication-entries")
public class DeduplicationEntryResource {

    private static final Logger LOG = LoggerFactory.getLogger(DeduplicationEntryResource.class);

    private final DeduplicationEntryService deduplicationEntryService;

    public DeduplicationEntryResource(DeduplicationEntryService deduplicationEntryService) {
        this.deduplicationEntryService = deduplicationEntryService;
    }

    /**
     * {@code GET  /deduplication-entries} : get all the deduplicationEntries.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of deduplicationEntries in body.
     */
    @GetMapping("")
    public List<DeduplicationEntryDTO> getAllDeduplicationEntries() {
        LOG.debug("REST request to get all DeduplicationEntries");
        return deduplicationEntryService.findAll();
    }

    /**
     * {@code GET  /deduplication-entries/:id} : get the "id" deduplicationEntry.
     *
     * @param id the id of the deduplicationEntryDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the deduplicationEntryDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DeduplicationEntryDTO> getDeduplicationEntry(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get DeduplicationEntry : {}", id);
        Optional<DeduplicationEntryDTO> deduplicationEntryDTO = deduplicationEntryService.findOne(id);
        return ResponseUtil.wrapOrNotFound(deduplicationEntryDTO);
    }
}
