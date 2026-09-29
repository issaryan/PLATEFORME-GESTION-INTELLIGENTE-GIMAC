package cm.gimac.des.certification.web.rest;

import cm.gimac.des.certification.repository.QuitusRepository;
import cm.gimac.des.certification.service.QuitusService;
import cm.gimac.des.certification.service.dto.QuitusDTO;
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
 * REST controller for managing {@link cm.gimac.des.certification.domain.Quitus}.
 */
@RestController
@RequestMapping("/api/quituses")
public class QuitusResource {

    private static final Logger LOG = LoggerFactory.getLogger(QuitusResource.class);

    private static final String ENTITY_NAME = "certificationQuitus";

    @Value("${jhipster.clientApp.name:certification}")
    private String applicationName;

    private final QuitusService quitusService;

    private final QuitusRepository quitusRepository;

    public QuitusResource(QuitusService quitusService, QuitusRepository quitusRepository) {
        this.quitusService = quitusService;
        this.quitusRepository = quitusRepository;
    }

    /**
     * {@code POST  /quituses} : Create a new quitus.
     *
     * @param quitusDTO the quitusDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new quitusDTO, or with status {@code 400 (Bad Request)} if the quitus already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<QuitusDTO> createQuitus(@Valid @RequestBody QuitusDTO quitusDTO) throws URISyntaxException {
        LOG.debug("REST request to save Quitus : {}", quitusDTO);
        if (quitusDTO.getId() != null) {
            throw new BadRequestAlertException("A new quitus cannot already have an ID", ENTITY_NAME, "idexists");
        }
        quitusDTO = quitusService.save(quitusDTO);
        return ResponseEntity.created(new URI("/api/quituses/" + quitusDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, quitusDTO.getId().toString()))
            .body(quitusDTO);
    }

    /**
     * {@code PUT  /quituses/:id} : Updates an existing quitus.
     *
     * @param id the id of the quitusDTO to save.
     * @param quitusDTO the quitusDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated quitusDTO,
     * or with status {@code 400 (Bad Request)} if the quitusDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the quitusDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<QuitusDTO> updateQuitus(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody QuitusDTO quitusDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Quitus : {}, {}", id, quitusDTO);
        if (quitusDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, quitusDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!quitusRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        quitusDTO = quitusService.update(quitusDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, quitusDTO.getId().toString()))
            .body(quitusDTO);
    }

    /**
     * {@code PATCH  /quituses/:id} : Partial updates given fields of an existing quitus, field will ignore if it is null
     *
     * @param id the id of the quitusDTO to save.
     * @param quitusDTO the quitusDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated quitusDTO,
     * or with status {@code 400 (Bad Request)} if the quitusDTO is not valid,
     * or with status {@code 404 (Not Found)} if the quitusDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the quitusDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<QuitusDTO> partialUpdateQuitus(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody QuitusDTO quitusDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partially update Quitus : {}, {}", id, quitusDTO);
        if (quitusDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, quitusDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!quitusRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<QuitusDTO> result = quitusService.partialUpdate(quitusDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, quitusDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /quituses} : get all the Quituses.
     *
     * @param filter the filter of the request.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Quituses in body.
     */
    @GetMapping("")
    public List<QuitusDTO> getAllQuituses(@RequestParam(name = "filter", required = false) String filter) {
        if ("signeddecision-is-null".equals(filter)) {
            LOG.debug("REST request to get all Quituss where signedDecision is null");
            return quitusService.findAllWhereSignedDecisionIsNull();
        }
        LOG.debug("REST request to get all Quituses");
        return quitusService.findAll();
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

    /**
     * {@code DELETE  /quituses/:id} : delete the "id" quitus.
     *
     * @param id the id of the quitusDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuitus(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete Quitus : {}", id);
        quitusService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
