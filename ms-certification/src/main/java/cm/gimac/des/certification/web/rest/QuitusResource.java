package cm.gimac.des.certification.web.rest;

import cm.gimac.des.certification.service.QuitusQueryService;
import cm.gimac.des.certification.service.QuitusService;
import cm.gimac.des.certification.service.criteria.QuitusCriteria;
import cm.gimac.des.certification.service.dto.QuitusDTO;
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
 * REST controller for managing {@link cm.gimac.des.certification.domain.Quitus}.
 */
@RestController
@RequestMapping("/api/quituses")
public class QuitusResource {

    private static final Logger LOG = LoggerFactory.getLogger(QuitusResource.class);

    private final QuitusService quitusService;

    private final QuitusQueryService quitusQueryService;

    public QuitusResource(QuitusService quitusService, QuitusQueryService quitusQueryService) {
        this.quitusService = quitusService;
        this.quitusQueryService = quitusQueryService;
    }

    /**
     * {@code GET  /quituses} : get all the quituses.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of quituses in body.
     */
    @GetMapping("")
    public ResponseEntity<List<QuitusDTO>> getAllQuituses(
        QuitusCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get Quituses by criteria: {}", criteria);

        Page<QuitusDTO> page = quitusQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /quituses/count} : count all the quituses.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countQuituses(QuitusCriteria criteria) {
        LOG.debug("REST request to count Quituses by criteria: {}", criteria);
        return ResponseEntity.ok().body(quitusQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /quituses/:id} : get the "id" quitus.
     *
     * @param id the id of the quitusDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the quitusDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<QuitusDTO> getQuitus(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get Quitus : {}", id);
        Optional<QuitusDTO> quitusDTO = quitusService.findOne(id);
        return ResponseUtil.wrapOrNotFound(quitusDTO);
    }
}
