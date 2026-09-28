package cm.gimac.des.rapprochement.web.rest;

import cm.gimac.des.rapprochement.service.StatementLineQueryService;
import cm.gimac.des.rapprochement.service.StatementLineService;
import cm.gimac.des.rapprochement.service.criteria.StatementLineCriteria;
import cm.gimac.des.rapprochement.service.dto.StatementLineDTO;
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
 * REST controller for managing {@link cm.gimac.des.rapprochement.domain.StatementLine}.
 */
@RestController
@RequestMapping("/api/statement-lines")
public class StatementLineResource {

    private static final Logger LOG = LoggerFactory.getLogger(StatementLineResource.class);

    private final StatementLineService statementLineService;

    private final StatementLineQueryService statementLineQueryService;

    public StatementLineResource(StatementLineService statementLineService, StatementLineQueryService statementLineQueryService) {
        this.statementLineService = statementLineService;
        this.statementLineQueryService = statementLineQueryService;
    }

    /**
     * {@code GET  /statement-lines} : get all the statementLines.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of statementLines in body.
     */
    @GetMapping("")
    public ResponseEntity<List<StatementLineDTO>> getAllStatementLines(
        StatementLineCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get StatementLines by criteria: {}", criteria);

        Page<StatementLineDTO> page = statementLineQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /statement-lines/count} : count all the statementLines.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countStatementLines(StatementLineCriteria criteria) {
        LOG.debug("REST request to count StatementLines by criteria: {}", criteria);
        return ResponseEntity.ok().body(statementLineQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /statement-lines/:id} : get the "id" statementLine.
     *
     * @param id the id of the statementLineDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the statementLineDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<StatementLineDTO> getStatementLine(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get StatementLine : {}", id);
        Optional<StatementLineDTO> statementLineDTO = statementLineService.findOne(id);
        return ResponseUtil.wrapOrNotFound(statementLineDTO);
    }
}
