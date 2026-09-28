package cm.gimac.des.notification.web.rest;

import cm.gimac.des.notification.service.ConsolidationWindowService;
import cm.gimac.des.notification.service.dto.ConsolidationWindowDTO;
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
 * REST controller for managing {@link cm.gimac.des.notification.domain.ConsolidationWindow}.
 */
@RestController
@RequestMapping("/api/consolidation-windows")
public class ConsolidationWindowResource {

    private static final Logger LOG = LoggerFactory.getLogger(ConsolidationWindowResource.class);

    private final ConsolidationWindowService consolidationWindowService;

    public ConsolidationWindowResource(ConsolidationWindowService consolidationWindowService) {
        this.consolidationWindowService = consolidationWindowService;
    }

    /**
     * {@code GET  /consolidation-windows} : get all the consolidationWindows.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of consolidationWindows in body.
     */
    @GetMapping("")
    public ResponseEntity<List<ConsolidationWindowDTO>> getAllConsolidationWindows(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get a page of ConsolidationWindows");
        Page<ConsolidationWindowDTO> page = consolidationWindowService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /consolidation-windows/:id} : get the "id" consolidationWindow.
     *
     * @param id the id of the consolidationWindowDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the consolidationWindowDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ConsolidationWindowDTO> getConsolidationWindow(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get ConsolidationWindow : {}", id);
        Optional<ConsolidationWindowDTO> consolidationWindowDTO = consolidationWindowService.findOne(id);
        return ResponseUtil.wrapOrNotFound(consolidationWindowDTO);
    }
}
