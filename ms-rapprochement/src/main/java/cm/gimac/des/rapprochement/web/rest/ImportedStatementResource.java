package cm.gimac.des.rapprochement.web.rest;

import cm.gimac.des.rapprochement.service.ImportedStatementQueryService;
import cm.gimac.des.rapprochement.service.ImportedStatementService;
import cm.gimac.des.rapprochement.service.criteria.ImportedStatementCriteria;
import cm.gimac.des.rapprochement.service.dto.ImportedStatementDTO;
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
 * REST controller for managing {@link cm.gimac.des.rapprochement.domain.ImportedStatement}.
 */
@RestController
@RequestMapping("/api/imported-statements")
public class ImportedStatementResource {

    private static final Logger LOG = LoggerFactory.getLogger(ImportedStatementResource.class);

    private final ImportedStatementService importedStatementService;

    private final ImportedStatementQueryService importedStatementQueryService;

    public ImportedStatementResource(
        ImportedStatementService importedStatementService,
        ImportedStatementQueryService importedStatementQueryService
    ) {
        this.importedStatementService = importedStatementService;
        this.importedStatementQueryService = importedStatementQueryService;
    }

    /**
     * {@code GET  /imported-statements} : get all the importedStatements.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of importedStatements in body.
     */
    @GetMapping("")
    public ResponseEntity<List<ImportedStatementDTO>> getAllImportedStatements(
        ImportedStatementCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get ImportedStatements by criteria: {}", criteria);

        Page<ImportedStatementDTO> page = importedStatementQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /imported-statements/count} : count all the importedStatements.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countImportedStatements(ImportedStatementCriteria criteria) {
        LOG.debug("REST request to count ImportedStatements by criteria: {}", criteria);
        return ResponseEntity.ok().body(importedStatementQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /imported-statements/:id} : get the "id" importedStatement.
     *
     * @param id the id of the importedStatementDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the importedStatementDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ImportedStatementDTO> getImportedStatement(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get ImportedStatement : {}", id);
        Optional<ImportedStatementDTO> importedStatementDTO = importedStatementService.findOne(id);
        return ResponseUtil.wrapOrNotFound(importedStatementDTO);
    }
}
