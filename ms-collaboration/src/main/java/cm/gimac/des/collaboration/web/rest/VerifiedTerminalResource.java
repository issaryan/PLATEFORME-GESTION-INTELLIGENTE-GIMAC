package cm.gimac.des.collaboration.web.rest;

import cm.gimac.des.collaboration.repository.VerifiedTerminalRepository;
import cm.gimac.des.collaboration.service.VerifiedTerminalQueryService;
import cm.gimac.des.collaboration.service.VerifiedTerminalService;
import cm.gimac.des.collaboration.service.criteria.VerifiedTerminalCriteria;
import cm.gimac.des.collaboration.service.dto.VerifiedTerminalDTO;
import cm.gimac.des.collaboration.web.rest.errors.BadRequestAlertException;
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
 * REST controller for managing {@link cm.gimac.des.collaboration.domain.VerifiedTerminal}.
 */
@RestController
@RequestMapping("/api/verified-terminals")
public class VerifiedTerminalResource {

    private static final Logger LOG = LoggerFactory.getLogger(VerifiedTerminalResource.class);

    private static final String ENTITY_NAME = "collaborationVerifiedTerminal";

    @Value("${jhipster.clientApp.name:collaboration}")
    private String applicationName;

    private final VerifiedTerminalService verifiedTerminalService;

    private final VerifiedTerminalRepository verifiedTerminalRepository;

    private final VerifiedTerminalQueryService verifiedTerminalQueryService;

    public VerifiedTerminalResource(
        VerifiedTerminalService verifiedTerminalService,
        VerifiedTerminalRepository verifiedTerminalRepository,
        VerifiedTerminalQueryService verifiedTerminalQueryService
    ) {
        this.verifiedTerminalService = verifiedTerminalService;
        this.verifiedTerminalRepository = verifiedTerminalRepository;
        this.verifiedTerminalQueryService = verifiedTerminalQueryService;
    }

    /**
     * {@code POST  /verified-terminals} : Create a new verifiedTerminal.
     *
     * @param verifiedTerminalDTO the verifiedTerminalDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new verifiedTerminalDTO, or with status {@code 400 (Bad Request)} if the verifiedTerminal already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<VerifiedTerminalDTO> createVerifiedTerminal(@Valid @RequestBody VerifiedTerminalDTO verifiedTerminalDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save VerifiedTerminal : {}", verifiedTerminalDTO);
        if (verifiedTerminalDTO.getId() != null) {
            throw new BadRequestAlertException("A new verifiedTerminal cannot already have an ID", ENTITY_NAME, "idexists");
        }
        verifiedTerminalDTO = verifiedTerminalService.save(verifiedTerminalDTO);
        return ResponseEntity.created(new URI("/api/verified-terminals/" + verifiedTerminalDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, verifiedTerminalDTO.getId().toString()))
            .body(verifiedTerminalDTO);
    }

    /**
     * {@code PUT  /verified-terminals/:id} : Updates an existing verifiedTerminal.
     *
     * @param id the id of the verifiedTerminalDTO to save.
     * @param verifiedTerminalDTO the verifiedTerminalDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated verifiedTerminalDTO,
     * or with status {@code 400 (Bad Request)} if the verifiedTerminalDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the verifiedTerminalDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<VerifiedTerminalDTO> updateVerifiedTerminal(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody VerifiedTerminalDTO verifiedTerminalDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update VerifiedTerminal : {}, {}", id, verifiedTerminalDTO);
        if (verifiedTerminalDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, verifiedTerminalDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!verifiedTerminalRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        verifiedTerminalDTO = verifiedTerminalService.update(verifiedTerminalDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, verifiedTerminalDTO.getId().toString()))
            .body(verifiedTerminalDTO);
    }

    /**
     * {@code PATCH  /verified-terminals/:id} : Partial updates given fields of an existing verifiedTerminal, field will ignore if it is null
     *
     * @param id the id of the verifiedTerminalDTO to save.
     * @param verifiedTerminalDTO the verifiedTerminalDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated verifiedTerminalDTO,
     * or with status {@code 400 (Bad Request)} if the verifiedTerminalDTO is not valid,
     * or with status {@code 404 (Not Found)} if the verifiedTerminalDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the verifiedTerminalDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<VerifiedTerminalDTO> partialUpdateVerifiedTerminal(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody VerifiedTerminalDTO verifiedTerminalDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partially update VerifiedTerminal : {}, {}", id, verifiedTerminalDTO);
        if (verifiedTerminalDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, verifiedTerminalDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!verifiedTerminalRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<VerifiedTerminalDTO> result = verifiedTerminalService.partialUpdate(verifiedTerminalDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, verifiedTerminalDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /verified-terminals} : get all the Verified Terminals.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Verified Terminals in body.
     */
    @GetMapping("")
    public ResponseEntity<List<VerifiedTerminalDTO>> getAllVerifiedTerminals(
        VerifiedTerminalCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get VerifiedTerminals by criteria: {}", criteria);

        Page<VerifiedTerminalDTO> page = verifiedTerminalQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /verified-terminals/count} : count all the verifiedTerminals.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countVerifiedTerminals(VerifiedTerminalCriteria criteria) {
        LOG.debug("REST request to count VerifiedTerminals by criteria: {}", criteria);
        return ResponseEntity.ok().body(verifiedTerminalQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /verified-terminals/:id} : get the "id" verifiedTerminal.
     *
     * @param id the id of the verifiedTerminalDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the verifiedTerminalDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<VerifiedTerminalDTO> getVerifiedTerminal(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get VerifiedTerminal : {}", id);
        Optional<VerifiedTerminalDTO> verifiedTerminalDTO = verifiedTerminalService.findOne(id);
        return ResponseUtil.wrapOrNotFound(verifiedTerminalDTO);
    }

    /**
     * {@code DELETE  /verified-terminals/:id} : delete the "id" verifiedTerminal.
     *
     * @param id the id of the verifiedTerminalDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVerifiedTerminal(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete VerifiedTerminal : {}", id);
        verifiedTerminalService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
