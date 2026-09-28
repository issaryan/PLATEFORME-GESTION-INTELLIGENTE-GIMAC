package cm.gimac.des.reporting.web.rest;

import cm.gimac.des.reporting.repository.ReportVisaRepository;
import cm.gimac.des.reporting.service.ReportVisaService;
import cm.gimac.des.reporting.service.dto.ReportVisaDTO;
import cm.gimac.des.reporting.web.rest.errors.BadRequestAlertException;
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
 * REST controller for managing {@link cm.gimac.des.reporting.domain.ReportVisa}.
 */
@RestController
@RequestMapping("/api/report-visas")
public class ReportVisaResource {

    private static final Logger LOG = LoggerFactory.getLogger(ReportVisaResource.class);

    private static final String ENTITY_NAME = "reportingReportVisa";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ReportVisaService reportVisaService;

    private final ReportVisaRepository reportVisaRepository;

    public ReportVisaResource(ReportVisaService reportVisaService, ReportVisaRepository reportVisaRepository) {
        this.reportVisaService = reportVisaService;
        this.reportVisaRepository = reportVisaRepository;
    }

    /**
     * {@code POST  /report-visas} : Create a new reportVisa.
     *
     * @param reportVisaDTO the reportVisaDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new reportVisaDTO, or with status {@code 400 (Bad Request)} if the reportVisa has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<ReportVisaDTO> createReportVisa(@Valid @RequestBody ReportVisaDTO reportVisaDTO) throws URISyntaxException {
        LOG.debug("REST request to save ReportVisa : {}", reportVisaDTO);
        if (reportVisaDTO.getId() != null) {
            throw new BadRequestAlertException("A new reportVisa cannot already have an ID", ENTITY_NAME, "idexists");
        }
        reportVisaDTO = reportVisaService.save(reportVisaDTO);
        return ResponseEntity.created(new URI("/api/report-visas/" + reportVisaDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, reportVisaDTO.getId().toString()))
            .body(reportVisaDTO);
    }

    /**
     * {@code PUT  /report-visas/:id} : Updates an existing reportVisa.
     *
     * @param id the id of the reportVisaDTO to save.
     * @param reportVisaDTO the reportVisaDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated reportVisaDTO,
     * or with status {@code 400 (Bad Request)} if the reportVisaDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the reportVisaDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ReportVisaDTO> updateReportVisa(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody ReportVisaDTO reportVisaDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update ReportVisa : {}, {}", id, reportVisaDTO);
        if (reportVisaDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, reportVisaDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!reportVisaRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        reportVisaDTO = reportVisaService.update(reportVisaDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, reportVisaDTO.getId().toString()))
            .body(reportVisaDTO);
    }

    /**
     * {@code PATCH  /report-visas/:id} : Partial updates given fields of an existing reportVisa, field will ignore if it is null
     *
     * @param id the id of the reportVisaDTO to save.
     * @param reportVisaDTO the reportVisaDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated reportVisaDTO,
     * or with status {@code 400 (Bad Request)} if the reportVisaDTO is not valid,
     * or with status {@code 404 (Not Found)} if the reportVisaDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the reportVisaDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<ReportVisaDTO> partialUpdateReportVisa(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody ReportVisaDTO reportVisaDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update ReportVisa partially : {}, {}", id, reportVisaDTO);
        if (reportVisaDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, reportVisaDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!reportVisaRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<ReportVisaDTO> result = reportVisaService.partialUpdate(reportVisaDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, reportVisaDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /report-visas} : get all the reportVisas.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of reportVisas in body.
     */
    @GetMapping("")
    public ResponseEntity<List<ReportVisaDTO>> getAllReportVisas(@org.springdoc.core.annotations.ParameterObject Pageable pageable) {
        LOG.debug("REST request to get a page of ReportVisas");
        Page<ReportVisaDTO> page = reportVisaService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /report-visas/:id} : get the "id" reportVisa.
     *
     * @param id the id of the reportVisaDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the reportVisaDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReportVisaDTO> getReportVisa(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get ReportVisa : {}", id);
        Optional<ReportVisaDTO> reportVisaDTO = reportVisaService.findOne(id);
        return ResponseUtil.wrapOrNotFound(reportVisaDTO);
    }

    /**
     * {@code DELETE  /report-visas/:id} : delete the "id" reportVisa.
     *
     * @param id the id of the reportVisaDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReportVisa(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete ReportVisa : {}", id);
        reportVisaService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
