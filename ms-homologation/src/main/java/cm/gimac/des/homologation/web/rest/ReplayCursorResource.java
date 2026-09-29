package cm.gimac.des.homologation.web.rest;

import cm.gimac.des.homologation.repository.ReplayCursorRepository;
import cm.gimac.des.homologation.service.ReplayCursorService;
import cm.gimac.des.homologation.service.dto.ReplayCursorDTO;
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
 * REST controller for managing {@link cm.gimac.des.homologation.domain.ReplayCursor}.
 */
@RestController
@RequestMapping("/api/replay-cursors")
public class ReplayCursorResource {

    private static final Logger LOG = LoggerFactory.getLogger(ReplayCursorResource.class);

    private static final String ENTITY_NAME = "homologationReplayCursor";

    @Value("${jhipster.clientApp.name:homologation}")
    private String applicationName;

    private final ReplayCursorService replayCursorService;

    private final ReplayCursorRepository replayCursorRepository;

    public ReplayCursorResource(ReplayCursorService replayCursorService, ReplayCursorRepository replayCursorRepository) {
        this.replayCursorService = replayCursorService;
        this.replayCursorRepository = replayCursorRepository;
    }

    /**
     * {@code POST  /replay-cursors} : Create a new replayCursor.
     *
     * @param replayCursorDTO the replayCursorDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new replayCursorDTO, or with status {@code 400 (Bad Request)} if the replayCursor already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<ReplayCursorDTO> createReplayCursor(@Valid @RequestBody ReplayCursorDTO replayCursorDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save ReplayCursor : {}", replayCursorDTO);
        if (replayCursorDTO.getId() != null) {
            throw new BadRequestAlertException("A new replayCursor cannot already have an ID", ENTITY_NAME, "idexists");
        }
        replayCursorDTO = replayCursorService.save(replayCursorDTO);
        return ResponseEntity.created(new URI("/api/replay-cursors/" + replayCursorDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, replayCursorDTO.getId().toString()))
            .body(replayCursorDTO);
    }

    /**
     * {@code PUT  /replay-cursors/:id} : Updates an existing replayCursor.
     *
     * @param id the id of the replayCursorDTO to save.
     * @param replayCursorDTO the replayCursorDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated replayCursorDTO,
     * or with status {@code 400 (Bad Request)} if the replayCursorDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the replayCursorDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ReplayCursorDTO> updateReplayCursor(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody ReplayCursorDTO replayCursorDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update ReplayCursor : {}, {}", id, replayCursorDTO);
        if (replayCursorDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, replayCursorDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!replayCursorRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        replayCursorDTO = replayCursorService.update(replayCursorDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, replayCursorDTO.getId().toString()))
            .body(replayCursorDTO);
    }

    /**
     * {@code PATCH  /replay-cursors/:id} : Partial updates given fields of an existing replayCursor, field will ignore if it is null
     *
     * @param id the id of the replayCursorDTO to save.
     * @param replayCursorDTO the replayCursorDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated replayCursorDTO,
     * or with status {@code 400 (Bad Request)} if the replayCursorDTO is not valid,
     * or with status {@code 404 (Not Found)} if the replayCursorDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the replayCursorDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<ReplayCursorDTO> partialUpdateReplayCursor(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody ReplayCursorDTO replayCursorDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partially update ReplayCursor : {}, {}", id, replayCursorDTO);
        if (replayCursorDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, replayCursorDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!replayCursorRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<ReplayCursorDTO> result = replayCursorService.partialUpdate(replayCursorDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, replayCursorDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /replay-cursors} : get all the Replay Cursors.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Replay Cursors in body.
     */
    @GetMapping("")
    public List<ReplayCursorDTO> getAllReplayCursors() {
        LOG.debug("REST request to get all ReplayCursors");
        return replayCursorService.findAll();
    }

    /**
     * {@code GET  /replay-cursors/:id} : get the "id" replayCursor.
     *
     * @param id the id of the replayCursorDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the replayCursorDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReplayCursorDTO> getReplayCursor(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get ReplayCursor : {}", id);
        Optional<ReplayCursorDTO> replayCursorDTO = replayCursorService.findOne(id);
        return ResponseUtil.wrapOrNotFound(replayCursorDTO);
    }

    /**
     * {@code DELETE  /replay-cursors/:id} : delete the "id" replayCursor.
     *
     * @param id the id of the replayCursorDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReplayCursor(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete ReplayCursor : {}", id);
        replayCursorService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
