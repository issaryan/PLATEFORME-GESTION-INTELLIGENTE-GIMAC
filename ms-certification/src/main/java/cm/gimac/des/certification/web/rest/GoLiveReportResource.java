package cm.gimac.des.certification.web.rest;

import cm.gimac.des.certification.repository.GoLiveReportRepository;
import cm.gimac.des.certification.service.GoLiveReportService;
import cm.gimac.des.certification.service.dto.GoLiveReportDTO;
import cm.gimac.des.certification.web.rest.errors.BadRequestAlertException;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link cm.gimac.des.certification.domain.GoLiveReport}.
 */
@RestController
@RequestMapping("/api/go-live-reports")
public class GoLiveReportResource {

    private static final Logger LOG = LoggerFactory.getLogger(GoLiveReportResource.class);

    private static final String ENTITY_NAME = "certificationGoLiveReport";

    @Value("${jhipster.clientApp.name:certification}")
    private String applicationName;

    private final GoLiveReportService goLiveReportService;

    private final GoLiveReportRepository goLiveReportRepository;

    public GoLiveReportResource(GoLiveReportService goLiveReportService, GoLiveReportRepository goLiveReportRepository) {
        this.goLiveReportService = goLiveReportService;
        this.goLiveReportRepository = goLiveReportRepository;
    }

    /**
     * {@code POST  /go-live-reports} : Create a new goLiveReport.
     *
     * @param goLiveReportDTO the goLiveReportDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new goLiveReportDTO, or with status {@code 400 (Bad Request)} if the goLiveReport already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<GoLiveReportDTO> createGoLiveReport(@Valid @RequestBody GoLiveReportDTO goLiveReportDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save GoLiveReport : {}", goLiveReportDTO);
        if (goLiveReportDTO.getId() != null) {
            throw new BadRequestAlertException("A new goLiveReport cannot already have an ID", ENTITY_NAME, "idexists");
        }
        goLiveReportDTO = goLiveReportService.save(goLiveReportDTO);
        return ResponseEntity.created(new URI("/api/go-live-reports/" + goLiveReportDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, goLiveReportDTO.getId().toString()))
            .body(goLiveReportDTO);
    }

    /**
     * {@code PUT  /go-live-reports/:id} : Updates an existing goLiveReport.
     *
     * @param id the id of the goLiveReportDTO to save.
     * @param goLiveReportDTO the goLiveReportDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated goLiveReportDTO,
     * or with status {@code 400 (Bad Request)} if the goLiveReportDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the goLiveReportDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<GoLiveReportDTO> updateGoLiveReport(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody GoLiveReportDTO goLiveReportDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update GoLiveReport : {}, {}", id, goLiveReportDTO);
        if (goLiveReportDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, goLiveReportDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!goLiveReportRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        goLiveReportDTO = goLiveReportService.update(goLiveReportDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, goLiveReportDTO.getId().toString()))
            .body(goLiveReportDTO);
    }

    /**
     * {@code PATCH  /go-live-reports/:id} : Partial updates given fields of an existing goLiveReport, field will ignore if it is null
     *
     * @param id the id of the goLiveReportDTO to save.
     * @param goLiveReportDTO the goLiveReportDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated goLiveReportDTO,
     * or with status {@code 400 (Bad Request)} if the goLiveReportDTO is not valid,
     * or with status {@code 404 (Not Found)} if the goLiveReportDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the goLiveReportDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<GoLiveReportDTO> partialUpdateGoLiveReport(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody GoLiveReportDTO goLiveReportDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partially update GoLiveReport : {}, {}", id, goLiveReportDTO);
        if (goLiveReportDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, goLiveReportDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!goLiveReportRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<GoLiveReportDTO> result = goLiveReportService.partialUpdate(goLiveReportDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, goLiveReportDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /go-live-reports} : get all the Go Live Reports.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Go Live Reports in body.
     */
    @GetMapping("")
    public List<GoLiveReportDTO> getAllGoLiveReports() {
        LOG.debug("REST request to get all GoLiveReports");
        return goLiveReportService.findAll();
    }

    /**
     * {@code GET  /go-live-reports/:id} : get the "id" goLiveReport.
     *
     * @param id the id of the goLiveReportDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the goLiveReportDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<GoLiveReportDTO> getGoLiveReport(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get GoLiveReport : {}", id);
        Optional<GoLiveReportDTO> goLiveReportDTO = goLiveReportService.findOne(id);
        return ResponseUtil.wrapOrNotFound(goLiveReportDTO);
    }

    /**
     * {@code DELETE  /go-live-reports/:id} : delete the "id" goLiveReport.
     *
     * @param id the id of the goLiveReportDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGoLiveReport(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete GoLiveReport : {}", id);
        goLiveReportService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
