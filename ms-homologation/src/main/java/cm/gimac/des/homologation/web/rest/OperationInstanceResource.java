package cm.gimac.des.homologation.web.rest;

import cm.gimac.des.homologation.repository.OperationInstanceRepository;
import cm.gimac.des.homologation.service.OperationInstanceService;
import cm.gimac.des.homologation.service.dto.OperationInstanceDTO;
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
 * REST controller for managing {@link cm.gimac.des.homologation.domain.OperationInstance}.
 */
@RestController
@RequestMapping("/api/operation-instances")
public class OperationInstanceResource {

    private static final Logger LOG = LoggerFactory.getLogger(OperationInstanceResource.class);

    private static final String ENTITY_NAME = "homologationOperationInstance";

    @Value("${jhipster.clientApp.name:homologation}")
    private String applicationName;

    private final OperationInstanceService operationInstanceService;

    private final OperationInstanceRepository operationInstanceRepository;

    public OperationInstanceResource(
        OperationInstanceService operationInstanceService,
        OperationInstanceRepository operationInstanceRepository
    ) {
        this.operationInstanceService = operationInstanceService;
        this.operationInstanceRepository = operationInstanceRepository;
    }

    /**
     * {@code POST  /operation-instances} : Create a new operationInstance.
     *
     * @param operationInstanceDTO the operationInstanceDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new operationInstanceDTO, or with status {@code 400 (Bad Request)} if the operationInstance already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<OperationInstanceDTO> createOperationInstance(@Valid @RequestBody OperationInstanceDTO operationInstanceDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save OperationInstance : {}", operationInstanceDTO);
        if (operationInstanceDTO.getId() != null) {
            throw new BadRequestAlertException("A new operationInstance cannot already have an ID", ENTITY_NAME, "idexists");
        }
        operationInstanceDTO = operationInstanceService.save(operationInstanceDTO);
        return ResponseEntity.created(new URI("/api/operation-instances/" + operationInstanceDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, operationInstanceDTO.getId().toString()))
            .body(operationInstanceDTO);
    }

    /**
     * {@code PUT  /operation-instances/:id} : Updates an existing operationInstance.
     *
     * @param id the id of the operationInstanceDTO to save.
     * @param operationInstanceDTO the operationInstanceDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated operationInstanceDTO,
     * or with status {@code 400 (Bad Request)} if the operationInstanceDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the operationInstanceDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<OperationInstanceDTO> updateOperationInstance(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody OperationInstanceDTO operationInstanceDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update OperationInstance : {}, {}", id, operationInstanceDTO);
        if (operationInstanceDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, operationInstanceDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!operationInstanceRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        operationInstanceDTO = operationInstanceService.update(operationInstanceDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, operationInstanceDTO.getId().toString()))
            .body(operationInstanceDTO);
    }

    /**
     * {@code PATCH  /operation-instances/:id} : Partial updates given fields of an existing operationInstance, field will ignore if it is null
     *
     * @param id the id of the operationInstanceDTO to save.
     * @param operationInstanceDTO the operationInstanceDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated operationInstanceDTO,
     * or with status {@code 400 (Bad Request)} if the operationInstanceDTO is not valid,
     * or with status {@code 404 (Not Found)} if the operationInstanceDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the operationInstanceDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<OperationInstanceDTO> partialUpdateOperationInstance(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody OperationInstanceDTO operationInstanceDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partially update OperationInstance : {}, {}", id, operationInstanceDTO);
        if (operationInstanceDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, operationInstanceDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!operationInstanceRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<OperationInstanceDTO> result = operationInstanceService.partialUpdate(operationInstanceDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, operationInstanceDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /operation-instances} : get all the Operation Instances.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Operation Instances in body.
     */
    @GetMapping("")
    public ResponseEntity<List<OperationInstanceDTO>> getAllOperationInstances(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get a page of OperationInstances");
        Page<OperationInstanceDTO> page = operationInstanceService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /operation-instances/:id} : get the "id" operationInstance.
     *
     * @param id the id of the operationInstanceDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the operationInstanceDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<OperationInstanceDTO> getOperationInstance(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get OperationInstance : {}", id);
        Optional<OperationInstanceDTO> operationInstanceDTO = operationInstanceService.findOne(id);
        return ResponseUtil.wrapOrNotFound(operationInstanceDTO);
    }

    /**
     * {@code DELETE  /operation-instances/:id} : delete the "id" operationInstance.
     *
     * @param id the id of the operationInstanceDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOperationInstance(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete OperationInstance : {}", id);
        operationInstanceService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
