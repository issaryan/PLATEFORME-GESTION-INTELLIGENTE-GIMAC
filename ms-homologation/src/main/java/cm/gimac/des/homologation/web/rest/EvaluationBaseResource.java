package cm.gimac.des.homologation.web.rest;

import cm.gimac.des.homologation.repository.EvaluationBaseRepository;
import cm.gimac.des.homologation.service.EvaluationBaseService;
import cm.gimac.des.homologation.service.dto.EvaluationBaseDTO;
import cm.gimac.des.homologation.web.rest.errors.BadRequestAlertException;
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
 * REST controller for managing {@link cm.gimac.des.homologation.domain.EvaluationBase}.
 */
@RestController
@RequestMapping("/api/evaluation-bases")
public class EvaluationBaseResource {

    private static final Logger LOG = LoggerFactory.getLogger(EvaluationBaseResource.class);

    private static final String ENTITY_NAME = "homologationEvaluationBase";

    @Value("${jhipster.clientApp.name:homologation}")
    private String applicationName;

    private final EvaluationBaseService evaluationBaseService;

    private final EvaluationBaseRepository evaluationBaseRepository;

    public EvaluationBaseResource(EvaluationBaseService evaluationBaseService, EvaluationBaseRepository evaluationBaseRepository) {
        this.evaluationBaseService = evaluationBaseService;
        this.evaluationBaseRepository = evaluationBaseRepository;
    }

    /**
     * {@code POST  /evaluation-bases} : Create a new evaluationBase.
     *
     * @param evaluationBaseDTO the evaluationBaseDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new evaluationBaseDTO, or with status {@code 400 (Bad Request)} if the evaluationBase already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<EvaluationBaseDTO> createEvaluationBase(@Valid @RequestBody EvaluationBaseDTO evaluationBaseDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save EvaluationBase : {}", evaluationBaseDTO);
        if (evaluationBaseDTO.getId() != null) {
            throw new BadRequestAlertException("A new evaluationBase cannot already have an ID", ENTITY_NAME, "idexists");
        }
        evaluationBaseDTO = evaluationBaseService.save(evaluationBaseDTO);
        return ResponseEntity.created(new URI("/api/evaluation-bases/" + evaluationBaseDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, evaluationBaseDTO.getId().toString()))
            .body(evaluationBaseDTO);
    }

    /**
     * {@code PUT  /evaluation-bases/:id} : Updates an existing evaluationBase.
     *
     * @param id the id of the evaluationBaseDTO to save.
     * @param evaluationBaseDTO the evaluationBaseDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated evaluationBaseDTO,
     * or with status {@code 400 (Bad Request)} if the evaluationBaseDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the evaluationBaseDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<EvaluationBaseDTO> updateEvaluationBase(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody EvaluationBaseDTO evaluationBaseDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update EvaluationBase : {}, {}", id, evaluationBaseDTO);
        if (evaluationBaseDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, evaluationBaseDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!evaluationBaseRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        evaluationBaseDTO = evaluationBaseService.update(evaluationBaseDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, evaluationBaseDTO.getId().toString()))
            .body(evaluationBaseDTO);
    }

    /**
     * {@code PATCH  /evaluation-bases/:id} : Partial updates given fields of an existing evaluationBase, field will ignore if it is null
     *
     * @param id the id of the evaluationBaseDTO to save.
     * @param evaluationBaseDTO the evaluationBaseDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated evaluationBaseDTO,
     * or with status {@code 400 (Bad Request)} if the evaluationBaseDTO is not valid,
     * or with status {@code 404 (Not Found)} if the evaluationBaseDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the evaluationBaseDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<EvaluationBaseDTO> partialUpdateEvaluationBase(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody EvaluationBaseDTO evaluationBaseDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partially update EvaluationBase : {}, {}", id, evaluationBaseDTO);
        if (evaluationBaseDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, evaluationBaseDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!evaluationBaseRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<EvaluationBaseDTO> result = evaluationBaseService.partialUpdate(evaluationBaseDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, evaluationBaseDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /evaluation-bases} : get all the Evaluation Bases.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Evaluation Bases in body.
     */
    @GetMapping("")
    public List<EvaluationBaseDTO> getAllEvaluationBases() {
        LOG.debug("REST request to get all EvaluationBases");
        return evaluationBaseService.findAll();
    }

    /**
     * {@code GET  /evaluation-bases/:id} : get the "id" evaluationBase.
     *
     * @param id the id of the evaluationBaseDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the evaluationBaseDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<EvaluationBaseDTO> getEvaluationBase(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get EvaluationBase : {}", id);
        Optional<EvaluationBaseDTO> evaluationBaseDTO = evaluationBaseService.findOne(id);
        return ResponseUtil.wrapOrNotFound(evaluationBaseDTO);
    }

    /**
     * {@code DELETE  /evaluation-bases/:id} : delete the "id" evaluationBase.
     *
     * @param id the id of the evaluationBaseDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvaluationBase(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete EvaluationBase : {}", id);
        evaluationBaseService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
