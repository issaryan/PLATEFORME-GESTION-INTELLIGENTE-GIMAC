package cm.gimac.des.reporting.web.rest;

import cm.gimac.des.reporting.service.FactQueryService;
import cm.gimac.des.reporting.service.FactService;
import cm.gimac.des.reporting.service.criteria.FactCriteria;
import cm.gimac.des.reporting.service.dto.FactDTO;
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
 * REST controller for managing {@link cm.gimac.des.reporting.domain.Fact}.
 */
@RestController
@RequestMapping("/api/facts")
public class FactResource {

    private static final Logger LOG = LoggerFactory.getLogger(FactResource.class);

    private final FactService factService;

    private final FactQueryService factQueryService;

    public FactResource(FactService factService, FactQueryService factQueryService) {
        this.factService = factService;
        this.factQueryService = factQueryService;
    }

    /**
     * {@code GET  /facts} : get all the facts.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of facts in body.
     */
    @GetMapping("")
    public ResponseEntity<List<FactDTO>> getAllFacts(
        FactCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get Facts by criteria: {}", criteria);

        Page<FactDTO> page = factQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /facts/count} : count all the facts.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countFacts(FactCriteria criteria) {
        LOG.debug("REST request to count Facts by criteria: {}", criteria);
        return ResponseEntity.ok().body(factQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /facts/:id} : get the "id" fact.
     *
     * @param id the id of the factDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the factDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<FactDTO> getFact(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get Fact : {}", id);
        Optional<FactDTO> factDTO = factService.findOne(id);
        return ResponseUtil.wrapOrNotFound(factDTO);
    }
}
