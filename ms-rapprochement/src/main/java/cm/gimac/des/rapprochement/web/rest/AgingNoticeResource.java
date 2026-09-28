package cm.gimac.des.rapprochement.web.rest;

import cm.gimac.des.rapprochement.service.AgingNoticeService;
import cm.gimac.des.rapprochement.service.dto.AgingNoticeDTO;
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
 * REST controller for managing {@link cm.gimac.des.rapprochement.domain.AgingNotice}.
 */
@RestController
@RequestMapping("/api/aging-notices")
public class AgingNoticeResource {

    private static final Logger LOG = LoggerFactory.getLogger(AgingNoticeResource.class);

    private final AgingNoticeService agingNoticeService;

    public AgingNoticeResource(AgingNoticeService agingNoticeService) {
        this.agingNoticeService = agingNoticeService;
    }

    /**
     * {@code GET  /aging-notices} : get all the agingNotices.
     *
     * @param pageable the pagination information.
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of agingNotices in body.
     */
    @GetMapping("")
    public ResponseEntity<List<AgingNoticeDTO>> getAllAgingNotices(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get a page of AgingNotices");
        Page<AgingNoticeDTO> page;
        if (eagerload) {
            page = agingNoticeService.findAllWithEagerRelationships(pageable);
        } else {
            page = agingNoticeService.findAll(pageable);
        }
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /aging-notices/:id} : get the "id" agingNotice.
     *
     * @param id the id of the agingNoticeDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the agingNoticeDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AgingNoticeDTO> getAgingNotice(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get AgingNotice : {}", id);
        Optional<AgingNoticeDTO> agingNoticeDTO = agingNoticeService.findOne(id);
        return ResponseUtil.wrapOrNotFound(agingNoticeDTO);
    }
}
