package cm.gimac.des.homologation.web.rest;

import cm.gimac.des.homologation.repository.ContractualScopeRepository;
import cm.gimac.des.homologation.service.ContractualScopeService;
import cm.gimac.des.homologation.service.dto.ContractualScopeDTO;
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
 * REST controller for managing {@link cm.gimac.des.homologation.domain.ContractualScope}.
 */
@RestController
@RequestMapping("/api/contractual-scopes")
public class ContractualScopeResource {

    private static final Logger LOG = LoggerFactory.getLogger(ContractualScopeResource.class);

    private static final String ENTITY_NAME = "homologationContractualScope";

    @Value("${jhipster.clientApp.name:homologation}")
    private String applicationName;

    private final ContractualScopeService contractualScopeService;

    private final ContractualScopeRepository contractualScopeRepository;

    public ContractualScopeResource(
        ContractualScopeService contractualScopeService,
        ContractualScopeRepository contractualScopeRepository
    ) {
        this.contractualScopeService = contractualScopeService;
        this.contractualScopeRepository = contractualScopeRepository;
    }

    /**
     * {@code POST  /contractual-scopes} : Create a new contractualScope.
     *
     * @param contractualScopeDTO the contractualScopeDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new contractualScopeDTO, or with status {@code 400 (Bad Request)} if the contractualScope already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<ContractualScopeDTO> createContractualScope(@Valid @RequestBody ContractualScopeDTO contractualScopeDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save ContractualScope : {}", contractualScopeDTO);
        if (contractualScopeDTO.getId() != null) {
            throw new BadRequestAlertException("A new contractualScope cannot already have an ID", ENTITY_NAME, "idexists");
        }
        contractualScopeDTO = contractualScopeService.save(contractualScopeDTO);
        return ResponseEntity.created(new URI("/api/contractual-scopes/" + contractualScopeDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, contractualScopeDTO.getId().toString()))
            .body(contractualScopeDTO);
    }

    /**
     * {@code PUT  /contractual-scopes/:id} : Updates an existing contractualScope.
     *
     * @param id the id of the contractualScopeDTO to save.
     * @param contractualScopeDTO the contractualScopeDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated contractualScopeDTO,
     * or with status {@code 400 (Bad Request)} if the contractualScopeDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the contractualScopeDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ContractualScopeDTO> updateContractualScope(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody ContractualScopeDTO contractualScopeDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update ContractualScope : {}, {}", id, contractualScopeDTO);
        if (contractualScopeDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, contractualScopeDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!contractualScopeRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        contractualScopeDTO = contractualScopeService.update(contractualScopeDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, contractualScopeDTO.getId().toString()))
            .body(contractualScopeDTO);
    }

    /**
     * {@code PATCH  /contractual-scopes/:id} : Partial updates given fields of an existing contractualScope, field will ignore if it is null
     *
     * @param id the id of the contractualScopeDTO to save.
     * @param contractualScopeDTO the contractualScopeDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated contractualScopeDTO,
     * or with status {@code 400 (Bad Request)} if the contractualScopeDTO is not valid,
     * or with status {@code 404 (Not Found)} if the contractualScopeDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the contractualScopeDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<ContractualScopeDTO> partialUpdateContractualScope(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody ContractualScopeDTO contractualScopeDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partially update ContractualScope : {}, {}", id, contractualScopeDTO);
        if (contractualScopeDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, contractualScopeDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!contractualScopeRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<ContractualScopeDTO> result = contractualScopeService.partialUpdate(contractualScopeDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, contractualScopeDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /contractual-scopes} : get all the Contractual Scopes.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Contractual Scopes in body.
     */
    @GetMapping("")
    public List<ContractualScopeDTO> getAllContractualScopes() {
        LOG.debug("REST request to get all ContractualScopes");
        return contractualScopeService.findAll();
    }

    /**
     * {@code GET  /contractual-scopes/:id} : get the "id" contractualScope.
     *
     * @param id the id of the contractualScopeDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the contractualScopeDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ContractualScopeDTO> getContractualScope(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get ContractualScope : {}", id);
        Optional<ContractualScopeDTO> contractualScopeDTO = contractualScopeService.findOne(id);
        return ResponseUtil.wrapOrNotFound(contractualScopeDTO);
    }

    /**
     * {@code DELETE  /contractual-scopes/:id} : delete the "id" contractualScope.
     *
     * @param id the id of the contractualScopeDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContractualScope(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete ContractualScope : {}", id);
        contractualScopeService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
