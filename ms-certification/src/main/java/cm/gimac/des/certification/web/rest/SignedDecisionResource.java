package cm.gimac.des.certification.web.rest;

import cm.gimac.des.certification.repository.SignedDecisionRepository;
import cm.gimac.des.certification.service.SignedDecisionService;
import cm.gimac.des.certification.service.dto.SignedDecisionDTO;
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
 * REST controller for managing {@link cm.gimac.des.certification.domain.SignedDecision}.
 */
@RestController
@RequestMapping("/api/signed-decisions")
public class SignedDecisionResource {

    private static final Logger LOG = LoggerFactory.getLogger(SignedDecisionResource.class);

    private static final String ENTITY_NAME = "certificationSignedDecision";

    @Value("${jhipster.clientApp.name:certification}")
    private String applicationName;

    private final SignedDecisionService signedDecisionService;

    private final SignedDecisionRepository signedDecisionRepository;

    public SignedDecisionResource(SignedDecisionService signedDecisionService, SignedDecisionRepository signedDecisionRepository) {
        this.signedDecisionService = signedDecisionService;
        this.signedDecisionRepository = signedDecisionRepository;
    }

    /**
     * {@code POST  /signed-decisions} : Create a new signedDecision.
     *
     * @param signedDecisionDTO the signedDecisionDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new signedDecisionDTO, or with status {@code 400 (Bad Request)} if the signedDecision already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<SignedDecisionDTO> createSignedDecision(@Valid @RequestBody SignedDecisionDTO signedDecisionDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save SignedDecision : {}", signedDecisionDTO);
        if (signedDecisionDTO.getId() != null) {
            throw new BadRequestAlertException("A new signedDecision cannot already have an ID", ENTITY_NAME, "idexists");
        }
        signedDecisionDTO = signedDecisionService.save(signedDecisionDTO);
        return ResponseEntity.created(new URI("/api/signed-decisions/" + signedDecisionDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, signedDecisionDTO.getId().toString()))
            .body(signedDecisionDTO);
    }

    /**
     * {@code PUT  /signed-decisions/:id} : Updates an existing signedDecision.
     *
     * @param id the id of the signedDecisionDTO to save.
     * @param signedDecisionDTO the signedDecisionDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated signedDecisionDTO,
     * or with status {@code 400 (Bad Request)} if the signedDecisionDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the signedDecisionDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<SignedDecisionDTO> updateSignedDecision(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody SignedDecisionDTO signedDecisionDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update SignedDecision : {}, {}", id, signedDecisionDTO);
        if (signedDecisionDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, signedDecisionDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!signedDecisionRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        signedDecisionDTO = signedDecisionService.update(signedDecisionDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, signedDecisionDTO.getId().toString()))
            .body(signedDecisionDTO);
    }

    /**
     * {@code PATCH  /signed-decisions/:id} : Partial updates given fields of an existing signedDecision, field will ignore if it is null
     *
     * @param id the id of the signedDecisionDTO to save.
     * @param signedDecisionDTO the signedDecisionDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated signedDecisionDTO,
     * or with status {@code 400 (Bad Request)} if the signedDecisionDTO is not valid,
     * or with status {@code 404 (Not Found)} if the signedDecisionDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the signedDecisionDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<SignedDecisionDTO> partialUpdateSignedDecision(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody SignedDecisionDTO signedDecisionDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partially update SignedDecision : {}, {}", id, signedDecisionDTO);
        if (signedDecisionDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, signedDecisionDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!signedDecisionRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<SignedDecisionDTO> result = signedDecisionService.partialUpdate(signedDecisionDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, signedDecisionDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /signed-decisions} : get all the Signed Decisions.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Signed Decisions in body.
     */
    @GetMapping("")
    public List<SignedDecisionDTO> getAllSignedDecisions() {
        LOG.debug("REST request to get all SignedDecisions");
        return signedDecisionService.findAll();
    }

    /**
     * {@code GET  /signed-decisions/:id} : get the "id" signedDecision.
     *
     * @param id the id of the signedDecisionDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the signedDecisionDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SignedDecisionDTO> getSignedDecision(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get SignedDecision : {}", id);
        Optional<SignedDecisionDTO> signedDecisionDTO = signedDecisionService.findOne(id);
        return ResponseUtil.wrapOrNotFound(signedDecisionDTO);
    }

    /**
     * {@code DELETE  /signed-decisions/:id} : delete the "id" signedDecision.
     *
     * @param id the id of the signedDecisionDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSignedDecision(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete SignedDecision : {}", id);
        signedDecisionService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
