package cm.gimac.des.certification.web.rest;

import cm.gimac.des.certification.repository.GoLiveDossierRepository;
import cm.gimac.des.certification.service.GoLiveDossierService;
import cm.gimac.des.certification.service.dto.GoLiveDossierDTO;
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
 * REST controller for managing {@link cm.gimac.des.certification.domain.GoLiveDossier}.
 */
@RestController
@RequestMapping("/api/go-live-dossiers")
public class GoLiveDossierResource {

    private static final Logger LOG = LoggerFactory.getLogger(GoLiveDossierResource.class);

    private static final String ENTITY_NAME = "certificationGoLiveDossier";

    @Value("${jhipster.clientApp.name:certification}")
    private String applicationName;

    private final GoLiveDossierService goLiveDossierService;

    private final GoLiveDossierRepository goLiveDossierRepository;

    public GoLiveDossierResource(GoLiveDossierService goLiveDossierService, GoLiveDossierRepository goLiveDossierRepository) {
        this.goLiveDossierService = goLiveDossierService;
        this.goLiveDossierRepository = goLiveDossierRepository;
    }

    /**
     * {@code POST  /go-live-dossiers} : Create a new goLiveDossier.
     *
     * @param goLiveDossierDTO the goLiveDossierDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new goLiveDossierDTO, or with status {@code 400 (Bad Request)} if the goLiveDossier already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<GoLiveDossierDTO> createGoLiveDossier(@Valid @RequestBody GoLiveDossierDTO goLiveDossierDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save GoLiveDossier : {}", goLiveDossierDTO);
        if (goLiveDossierDTO.getId() != null) {
            throw new BadRequestAlertException("A new goLiveDossier cannot already have an ID", ENTITY_NAME, "idexists");
        }
        goLiveDossierDTO = goLiveDossierService.save(goLiveDossierDTO);
        return ResponseEntity.created(new URI("/api/go-live-dossiers/" + goLiveDossierDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, goLiveDossierDTO.getId().toString()))
            .body(goLiveDossierDTO);
    }

    /**
     * {@code PUT  /go-live-dossiers/:id} : Updates an existing goLiveDossier.
     *
     * @param id the id of the goLiveDossierDTO to save.
     * @param goLiveDossierDTO the goLiveDossierDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated goLiveDossierDTO,
     * or with status {@code 400 (Bad Request)} if the goLiveDossierDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the goLiveDossierDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<GoLiveDossierDTO> updateGoLiveDossier(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody GoLiveDossierDTO goLiveDossierDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update GoLiveDossier : {}, {}", id, goLiveDossierDTO);
        if (goLiveDossierDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, goLiveDossierDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!goLiveDossierRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        goLiveDossierDTO = goLiveDossierService.update(goLiveDossierDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, goLiveDossierDTO.getId().toString()))
            .body(goLiveDossierDTO);
    }

    /**
     * {@code PATCH  /go-live-dossiers/:id} : Partial updates given fields of an existing goLiveDossier, field will ignore if it is null
     *
     * @param id the id of the goLiveDossierDTO to save.
     * @param goLiveDossierDTO the goLiveDossierDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated goLiveDossierDTO,
     * or with status {@code 400 (Bad Request)} if the goLiveDossierDTO is not valid,
     * or with status {@code 404 (Not Found)} if the goLiveDossierDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the goLiveDossierDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<GoLiveDossierDTO> partialUpdateGoLiveDossier(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody GoLiveDossierDTO goLiveDossierDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partially update GoLiveDossier : {}, {}", id, goLiveDossierDTO);
        if (goLiveDossierDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, goLiveDossierDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!goLiveDossierRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<GoLiveDossierDTO> result = goLiveDossierService.partialUpdate(goLiveDossierDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, goLiveDossierDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /go-live-dossiers} : get all the Go Live Dossiers.
     *
     * @param filter the filter of the request.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Go Live Dossiers in body.
     */
    @GetMapping("")
    public List<GoLiveDossierDTO> getAllGoLiveDossiers(@RequestParam(name = "filter", required = false) String filter) {
        if ("golivereport-is-null".equals(filter)) {
            LOG.debug("REST request to get all GoLiveDossiers where goLiveReport is null");
            return goLiveDossierService.findAllWhereGoLiveReportIsNull();
        }

        if ("attestation-is-null".equals(filter)) {
            LOG.debug("REST request to get all GoLiveDossiers where attestation is null");
            return goLiveDossierService.findAllWhereAttestationIsNull();
        }
        LOG.debug("REST request to get all GoLiveDossiers");
        return goLiveDossierService.findAll();
    }

    /**
     * {@code GET  /go-live-dossiers/:id} : get the "id" goLiveDossier.
     *
     * @param id the id of the goLiveDossierDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the goLiveDossierDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<GoLiveDossierDTO> getGoLiveDossier(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get GoLiveDossier : {}", id);
        Optional<GoLiveDossierDTO> goLiveDossierDTO = goLiveDossierService.findOne(id);
        return ResponseUtil.wrapOrNotFound(goLiveDossierDTO);
    }

    /**
     * {@code DELETE  /go-live-dossiers/:id} : delete the "id" goLiveDossier.
     *
     * @param id the id of the goLiveDossierDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGoLiveDossier(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete GoLiveDossier : {}", id);
        goLiveDossierService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
