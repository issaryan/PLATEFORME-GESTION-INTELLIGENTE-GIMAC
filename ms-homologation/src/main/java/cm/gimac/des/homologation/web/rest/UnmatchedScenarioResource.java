package cm.gimac.des.homologation.web.rest;

import cm.gimac.des.homologation.repository.UnmatchedScenarioRepository;
import cm.gimac.des.homologation.service.UnmatchedScenarioService;
import cm.gimac.des.homologation.service.dto.UnmatchedScenarioDTO;
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
 * REST controller for managing {@link cm.gimac.des.homologation.domain.UnmatchedScenario}.
 */
@RestController
@RequestMapping("/api/unmatched-scenarios")
public class UnmatchedScenarioResource {

    private static final Logger LOG = LoggerFactory.getLogger(UnmatchedScenarioResource.class);

    private static final String ENTITY_NAME = "homologationUnmatchedScenario";

    @Value("${jhipster.clientApp.name:homologation}")
    private String applicationName;

    private final UnmatchedScenarioService unmatchedScenarioService;

    private final UnmatchedScenarioRepository unmatchedScenarioRepository;

    public UnmatchedScenarioResource(
        UnmatchedScenarioService unmatchedScenarioService,
        UnmatchedScenarioRepository unmatchedScenarioRepository
    ) {
        this.unmatchedScenarioService = unmatchedScenarioService;
        this.unmatchedScenarioRepository = unmatchedScenarioRepository;
    }

    /**
     * {@code POST  /unmatched-scenarios} : Create a new unmatchedScenario.
     *
     * @param unmatchedScenarioDTO the unmatchedScenarioDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new unmatchedScenarioDTO, or with status {@code 400 (Bad Request)} if the unmatchedScenario already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<UnmatchedScenarioDTO> createUnmatchedScenario(@Valid @RequestBody UnmatchedScenarioDTO unmatchedScenarioDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save UnmatchedScenario : {}", unmatchedScenarioDTO);
        if (unmatchedScenarioDTO.getId() != null) {
            throw new BadRequestAlertException("A new unmatchedScenario cannot already have an ID", ENTITY_NAME, "idexists");
        }
        unmatchedScenarioDTO = unmatchedScenarioService.save(unmatchedScenarioDTO);
        return ResponseEntity.created(new URI("/api/unmatched-scenarios/" + unmatchedScenarioDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, unmatchedScenarioDTO.getId().toString()))
            .body(unmatchedScenarioDTO);
    }

    /**
     * {@code PUT  /unmatched-scenarios/:id} : Updates an existing unmatchedScenario.
     *
     * @param id the id of the unmatchedScenarioDTO to save.
     * @param unmatchedScenarioDTO the unmatchedScenarioDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated unmatchedScenarioDTO,
     * or with status {@code 400 (Bad Request)} if the unmatchedScenarioDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the unmatchedScenarioDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<UnmatchedScenarioDTO> updateUnmatchedScenario(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody UnmatchedScenarioDTO unmatchedScenarioDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update UnmatchedScenario : {}, {}", id, unmatchedScenarioDTO);
        if (unmatchedScenarioDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, unmatchedScenarioDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!unmatchedScenarioRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        unmatchedScenarioDTO = unmatchedScenarioService.update(unmatchedScenarioDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, unmatchedScenarioDTO.getId().toString()))
            .body(unmatchedScenarioDTO);
    }

    /**
     * {@code PATCH  /unmatched-scenarios/:id} : Partial updates given fields of an existing unmatchedScenario, field will ignore if it is null
     *
     * @param id the id of the unmatchedScenarioDTO to save.
     * @param unmatchedScenarioDTO the unmatchedScenarioDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated unmatchedScenarioDTO,
     * or with status {@code 400 (Bad Request)} if the unmatchedScenarioDTO is not valid,
     * or with status {@code 404 (Not Found)} if the unmatchedScenarioDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the unmatchedScenarioDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<UnmatchedScenarioDTO> partialUpdateUnmatchedScenario(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody UnmatchedScenarioDTO unmatchedScenarioDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partially update UnmatchedScenario : {}, {}", id, unmatchedScenarioDTO);
        if (unmatchedScenarioDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, unmatchedScenarioDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!unmatchedScenarioRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<UnmatchedScenarioDTO> result = unmatchedScenarioService.partialUpdate(unmatchedScenarioDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, unmatchedScenarioDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /unmatched-scenarios} : get all the Unmatched Scenarios.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Unmatched Scenarios in body.
     */
    @GetMapping("")
    public ResponseEntity<List<UnmatchedScenarioDTO>> getAllUnmatchedScenarios(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get a page of UnmatchedScenarios");
        Page<UnmatchedScenarioDTO> page = unmatchedScenarioService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /unmatched-scenarios/:id} : get the "id" unmatchedScenario.
     *
     * @param id the id of the unmatchedScenarioDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the unmatchedScenarioDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UnmatchedScenarioDTO> getUnmatchedScenario(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get UnmatchedScenario : {}", id);
        Optional<UnmatchedScenarioDTO> unmatchedScenarioDTO = unmatchedScenarioService.findOne(id);
        return ResponseUtil.wrapOrNotFound(unmatchedScenarioDTO);
    }

    /**
     * {@code DELETE  /unmatched-scenarios/:id} : delete the "id" unmatchedScenario.
     *
     * @param id the id of the unmatchedScenarioDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUnmatchedScenario(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete UnmatchedScenario : {}", id);
        unmatchedScenarioService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
