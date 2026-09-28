package cm.gimac.des.reporting.web.rest;

import cm.gimac.des.reporting.service.ProcessedEventService;
import cm.gimac.des.reporting.service.dto.ProcessedEventDTO;
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
 * REST controller for managing {@link cm.gimac.des.reporting.domain.ProcessedEvent}.
 */
@RestController
@RequestMapping("/api/processed-events")
public class ProcessedEventResource {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessedEventResource.class);

    private final ProcessedEventService processedEventService;

    public ProcessedEventResource(ProcessedEventService processedEventService) {
        this.processedEventService = processedEventService;
    }

    /**
     * {@code GET  /processed-events} : get all the processedEvents.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of processedEvents in body.
     */
    @GetMapping("")
    public ResponseEntity<List<ProcessedEventDTO>> getAllProcessedEvents(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get a page of ProcessedEvents");
        Page<ProcessedEventDTO> page = processedEventService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /processed-events/:id} : get the "id" processedEvent.
     *
     * @param id the id of the processedEventDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the processedEventDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProcessedEventDTO> getProcessedEvent(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get ProcessedEvent : {}", id);
        Optional<ProcessedEventDTO> processedEventDTO = processedEventService.findOne(id);
        return ResponseUtil.wrapOrNotFound(processedEventDTO);
    }
}
