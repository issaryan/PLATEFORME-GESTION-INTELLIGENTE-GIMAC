package cm.gimac.des.pilotage.web.rest;

import cm.gimac.des.pilotage.repository.RiskRepository;
import cm.gimac.des.pilotage.service.RiskQueryService;
import cm.gimac.des.pilotage.service.RiskService;
import cm.gimac.des.pilotage.service.criteria.RiskCriteria;
import cm.gimac.des.pilotage.service.dto.RiskDTO;
import cm.gimac.des.pilotage.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link cm.gimac.des.pilotage.domain.Risk}.
 */
@RestController
@RequestMapping("/api/risks")
public class RiskResource {

    private static final Logger LOG = LoggerFactory.getLogger(RiskResource.class);

    private static final String ENTITY_NAME = "pilotageRisk";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final RiskService riskService;

    private final RiskRepository riskRepository;

    private final RiskQueryService riskQueryService;

    public RiskResource(RiskService riskService, RiskRepository riskRepository, RiskQueryService riskQueryService) {
        this.riskService = riskService;
        this.riskRepository = riskRepository;
        this.riskQueryService = riskQueryService;
    }

    /**
     * {@code POST  /risks} : Create a new risk.
     *
     * @param riskDTO the riskDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new riskDTO, or with status {@code 400 (Bad Request)} if the risk has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<RiskDTO> createRisk(@Valid @RequestBody RiskDTO riskDTO) throws URISyntaxException {
        LOG.debug("REST request to save Risk : {}", riskDTO);
        if (riskDTO.getId() != null) {
            throw new BadRequestAlertException("A new risk cannot already have an ID", ENTITY_NAME, "idexists");
        }
        riskDTO = riskService.save(riskDTO);
        return ResponseEntity.created(new URI("/api/risks/" + riskDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, riskDTO.getId().toString()))
            .body(riskDTO);
    }

    /**
     * {@code PUT  /risks/:id} : Updates an existing risk.
     *
     * @param id the id of the riskDTO to save.
     * @param riskDTO the riskDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated riskDTO,
     * or with status {@code 400 (Bad Request)} if the riskDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the riskDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<RiskDTO> updateRisk(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody RiskDTO riskDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Risk : {}, {}", id, riskDTO);
        if (riskDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, riskDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!riskRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        riskDTO = riskService.update(riskDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, riskDTO.getId().toString()))
            .body(riskDTO);
    }

    /**
     * {@code PATCH  /risks/:id} : Partial updates given fields of an existing risk, field will ignore if it is null
     *
     * @param id the id of the riskDTO to save.
     * @param riskDTO the riskDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated riskDTO,
     * or with status {@code 400 (Bad Request)} if the riskDTO is not valid,
     * or with status {@code 404 (Not Found)} if the riskDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the riskDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<RiskDTO> partialUpdateRisk(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody RiskDTO riskDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Risk partially : {}, {}", id, riskDTO);
        if (riskDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, riskDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!riskRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<RiskDTO> result = riskService.partialUpdate(riskDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, riskDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /risks} : get all the risks.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of risks in body.
     */
    @GetMapping("")
    public ResponseEntity<List<RiskDTO>> getAllRisks(
        RiskCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get Risks by criteria: {}", criteria);

        Page<RiskDTO> page = riskQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /risks/count} : count all the risks.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countRisks(RiskCriteria criteria) {
        LOG.debug("REST request to count Risks by criteria: {}", criteria);
        return ResponseEntity.ok().body(riskQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /risks/:id} : get the "id" risk.
     *
     * @param id the id of the riskDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the riskDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RiskDTO> getRisk(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get Risk : {}", id);
        Optional<RiskDTO> riskDTO = riskService.findOne(id);
        return ResponseUtil.wrapOrNotFound(riskDTO);
    }

    /**
     * {@code DELETE  /risks/:id} : delete the "id" risk.
     *
     * @param id the id of the riskDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRisk(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete Risk : {}", id);
        riskService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
