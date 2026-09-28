package cm.gimac.des.rapprochement.web.rest;

import cm.gimac.des.rapprochement.repository.DepositContractRepository;
import cm.gimac.des.rapprochement.service.DepositContractService;
import cm.gimac.des.rapprochement.service.dto.DepositContractDTO;
import cm.gimac.des.rapprochement.web.rest.errors.BadRequestAlertException;
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
 * REST controller for managing {@link cm.gimac.des.rapprochement.domain.DepositContract}.
 */
@RestController
@RequestMapping("/api/deposit-contracts")
public class DepositContractResource {

    private static final Logger LOG = LoggerFactory.getLogger(DepositContractResource.class);

    private static final String ENTITY_NAME = "rapprochementDepositContract";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final DepositContractService depositContractService;

    private final DepositContractRepository depositContractRepository;

    public DepositContractResource(DepositContractService depositContractService, DepositContractRepository depositContractRepository) {
        this.depositContractService = depositContractService;
        this.depositContractRepository = depositContractRepository;
    }

    /**
     * {@code POST  /deposit-contracts} : Create a new depositContract.
     *
     * @param depositContractDTO the depositContractDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new depositContractDTO, or with status {@code 400 (Bad Request)} if the depositContract has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<DepositContractDTO> createDepositContract(@Valid @RequestBody DepositContractDTO depositContractDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save DepositContract : {}", depositContractDTO);
        if (depositContractDTO.getId() != null) {
            throw new BadRequestAlertException("A new depositContract cannot already have an ID", ENTITY_NAME, "idexists");
        }
        depositContractDTO = depositContractService.save(depositContractDTO);
        return ResponseEntity.created(new URI("/api/deposit-contracts/" + depositContractDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, depositContractDTO.getId().toString()))
            .body(depositContractDTO);
    }

    /**
     * {@code PUT  /deposit-contracts/:id} : Updates an existing depositContract.
     *
     * @param id the id of the depositContractDTO to save.
     * @param depositContractDTO the depositContractDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated depositContractDTO,
     * or with status {@code 400 (Bad Request)} if the depositContractDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the depositContractDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<DepositContractDTO> updateDepositContract(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody DepositContractDTO depositContractDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update DepositContract : {}, {}", id, depositContractDTO);
        if (depositContractDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, depositContractDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!depositContractRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        depositContractDTO = depositContractService.update(depositContractDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, depositContractDTO.getId().toString()))
            .body(depositContractDTO);
    }

    /**
     * {@code PATCH  /deposit-contracts/:id} : Partial updates given fields of an existing depositContract, field will ignore if it is null
     *
     * @param id the id of the depositContractDTO to save.
     * @param depositContractDTO the depositContractDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated depositContractDTO,
     * or with status {@code 400 (Bad Request)} if the depositContractDTO is not valid,
     * or with status {@code 404 (Not Found)} if the depositContractDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the depositContractDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<DepositContractDTO> partialUpdateDepositContract(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody DepositContractDTO depositContractDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update DepositContract partially : {}, {}", id, depositContractDTO);
        if (depositContractDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, depositContractDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!depositContractRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<DepositContractDTO> result = depositContractService.partialUpdate(depositContractDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, depositContractDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /deposit-contracts} : get all the depositContracts.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of depositContracts in body.
     */
    @GetMapping("")
    public List<DepositContractDTO> getAllDepositContracts() {
        LOG.debug("REST request to get all DepositContracts");
        return depositContractService.findAll();
    }

    /**
     * {@code GET  /deposit-contracts/:id} : get the "id" depositContract.
     *
     * @param id the id of the depositContractDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the depositContractDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DepositContractDTO> getDepositContract(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get DepositContract : {}", id);
        Optional<DepositContractDTO> depositContractDTO = depositContractService.findOne(id);
        return ResponseUtil.wrapOrNotFound(depositContractDTO);
    }

    /**
     * {@code DELETE  /deposit-contracts/:id} : delete the "id" depositContract.
     *
     * @param id the id of the depositContractDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDepositContract(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete DepositContract : {}", id);
        depositContractService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
