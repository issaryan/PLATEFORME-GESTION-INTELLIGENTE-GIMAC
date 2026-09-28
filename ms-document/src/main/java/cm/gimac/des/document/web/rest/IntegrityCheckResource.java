package cm.gimac.des.document.web.rest;

import cm.gimac.des.document.service.IntegrityCheckService;
import cm.gimac.des.document.service.dto.IntegrityCheckDTO;
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
 * REST controller for managing {@link cm.gimac.des.document.domain.IntegrityCheck}.
 */
@RestController
@RequestMapping("/api/integrity-checks")
public class IntegrityCheckResource {

    private static final Logger LOG = LoggerFactory.getLogger(IntegrityCheckResource.class);

    private final IntegrityCheckService integrityCheckService;

    public IntegrityCheckResource(IntegrityCheckService integrityCheckService) {
        this.integrityCheckService = integrityCheckService;
    }

    /**
     * {@code GET  /integrity-checks} : get all the integrityChecks.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of integrityChecks in body.
     */
    @GetMapping("")
    public ResponseEntity<List<IntegrityCheckDTO>> getAllIntegrityChecks(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get a page of IntegrityChecks");
        Page<IntegrityCheckDTO> page = integrityCheckService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /integrity-checks/:id} : get the "id" integrityCheck.
     *
     * @param id the id of the integrityCheckDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the integrityCheckDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<IntegrityCheckDTO> getIntegrityCheck(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get IntegrityCheck : {}", id);
        Optional<IntegrityCheckDTO> integrityCheckDTO = integrityCheckService.findOne(id);
        return ResponseUtil.wrapOrNotFound(integrityCheckDTO);
    }
}
