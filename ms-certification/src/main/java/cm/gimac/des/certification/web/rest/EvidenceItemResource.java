package cm.gimac.des.certification.web.rest;

import cm.gimac.des.certification.service.EvidenceItemQueryService;
import cm.gimac.des.certification.service.EvidenceItemService;
import cm.gimac.des.certification.service.criteria.EvidenceItemCriteria;
import cm.gimac.des.certification.service.dto.EvidenceItemDTO;
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
 * REST controller for managing {@link cm.gimac.des.certification.domain.EvidenceItem}.
 */
@RestController
@RequestMapping("/api/evidence-items")
public class EvidenceItemResource {

    private static final Logger LOG = LoggerFactory.getLogger(EvidenceItemResource.class);

    private final EvidenceItemService evidenceItemService;

    private final EvidenceItemQueryService evidenceItemQueryService;

    public EvidenceItemResource(EvidenceItemService evidenceItemService, EvidenceItemQueryService evidenceItemQueryService) {
        this.evidenceItemService = evidenceItemService;
        this.evidenceItemQueryService = evidenceItemQueryService;
    }

    /**
     * {@code GET  /evidence-items} : get all the evidenceItems.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of evidenceItems in body.
     */
    @GetMapping("")
    public ResponseEntity<List<EvidenceItemDTO>> getAllEvidenceItems(
        EvidenceItemCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get EvidenceItems by criteria: {}", criteria);

        Page<EvidenceItemDTO> page = evidenceItemQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /evidence-items/count} : count all the evidenceItems.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countEvidenceItems(EvidenceItemCriteria criteria) {
        LOG.debug("REST request to count EvidenceItems by criteria: {}", criteria);
        return ResponseEntity.ok().body(evidenceItemQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /evidence-items/:id} : get the "id" evidenceItem.
     *
     * @param id the id of the evidenceItemDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the evidenceItemDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<EvidenceItemDTO> getEvidenceItem(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get EvidenceItem : {}", id);
        Optional<EvidenceItemDTO> evidenceItemDTO = evidenceItemService.findOne(id);
        return ResponseUtil.wrapOrNotFound(evidenceItemDTO);
    }
}
