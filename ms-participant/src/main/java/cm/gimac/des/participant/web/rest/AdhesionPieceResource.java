package cm.gimac.des.participant.web.rest;

import cm.gimac.des.participant.repository.AdhesionPieceRepository;
import cm.gimac.des.participant.service.AdhesionPieceService;
import cm.gimac.des.participant.service.dto.AdhesionPieceDTO;
import cm.gimac.des.participant.web.rest.errors.BadRequestAlertException;
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
 * REST controller for managing {@link cm.gimac.des.participant.domain.AdhesionPiece}.
 */
@RestController
@RequestMapping("/api/adhesion-pieces")
public class AdhesionPieceResource {

    private static final Logger LOG = LoggerFactory.getLogger(AdhesionPieceResource.class);

    private static final String ENTITY_NAME = "participantAdhesionPiece";

    @Value("${jhipster.clientApp.name:participant}")
    private String applicationName;

    private final AdhesionPieceService adhesionPieceService;

    private final AdhesionPieceRepository adhesionPieceRepository;

    public AdhesionPieceResource(AdhesionPieceService adhesionPieceService, AdhesionPieceRepository adhesionPieceRepository) {
        this.adhesionPieceService = adhesionPieceService;
        this.adhesionPieceRepository = adhesionPieceRepository;
    }

    /**
     * {@code POST  /adhesion-pieces} : Create a new adhesionPiece.
     *
     * @param adhesionPieceDTO the adhesionPieceDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new adhesionPieceDTO, or with status {@code 400 (Bad Request)} if the adhesionPiece already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<AdhesionPieceDTO> createAdhesionPiece(@Valid @RequestBody AdhesionPieceDTO adhesionPieceDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save AdhesionPiece : {}", adhesionPieceDTO);
        if (adhesionPieceDTO.getId() != null) {
            throw new BadRequestAlertException("A new adhesionPiece cannot already have an ID", ENTITY_NAME, "idexists");
        }
        adhesionPieceDTO = adhesionPieceService.save(adhesionPieceDTO);
        return ResponseEntity.created(new URI("/api/adhesion-pieces/" + adhesionPieceDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, adhesionPieceDTO.getId().toString()))
            .body(adhesionPieceDTO);
    }

    /**
     * {@code PUT  /adhesion-pieces/:id} : Updates an existing adhesionPiece.
     *
     * @param id the id of the adhesionPieceDTO to save.
     * @param adhesionPieceDTO the adhesionPieceDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated adhesionPieceDTO,
     * or with status {@code 400 (Bad Request)} if the adhesionPieceDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the adhesionPieceDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<AdhesionPieceDTO> updateAdhesionPiece(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody AdhesionPieceDTO adhesionPieceDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update AdhesionPiece : {}, {}", id, adhesionPieceDTO);
        if (adhesionPieceDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, adhesionPieceDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!adhesionPieceRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        adhesionPieceDTO = adhesionPieceService.update(adhesionPieceDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, adhesionPieceDTO.getId().toString()))
            .body(adhesionPieceDTO);
    }

    /**
     * {@code PATCH  /adhesion-pieces/:id} : Partial updates given fields of an existing adhesionPiece, field will ignore if it is null
     *
     * @param id the id of the adhesionPieceDTO to save.
     * @param adhesionPieceDTO the adhesionPieceDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated adhesionPieceDTO,
     * or with status {@code 400 (Bad Request)} if the adhesionPieceDTO is not valid,
     * or with status {@code 404 (Not Found)} if the adhesionPieceDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the adhesionPieceDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<AdhesionPieceDTO> partialUpdateAdhesionPiece(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody AdhesionPieceDTO adhesionPieceDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partially update AdhesionPiece : {}, {}", id, adhesionPieceDTO);
        if (adhesionPieceDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, adhesionPieceDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!adhesionPieceRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<AdhesionPieceDTO> result = adhesionPieceService.partialUpdate(adhesionPieceDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, adhesionPieceDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /adhesion-pieces} : get all the Adhesion Pieces.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Adhesion Pieces in body.
     */
    @GetMapping("")
    public List<AdhesionPieceDTO> getAllAdhesionPieces(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all AdhesionPieces");
        if (eagerload) {
            return adhesionPieceService.findAllWithEagerRelationships();
        } else {
            return adhesionPieceService.findAll();
        }
    }

    /**
     * {@code GET  /adhesion-pieces/:id} : get the "id" adhesionPiece.
     *
     * @param id the id of the adhesionPieceDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the adhesionPieceDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AdhesionPieceDTO> getAdhesionPiece(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get AdhesionPiece : {}", id);
        Optional<AdhesionPieceDTO> adhesionPieceDTO = adhesionPieceService.findOne(id);
        return ResponseUtil.wrapOrNotFound(adhesionPieceDTO);
    }

    /**
     * {@code DELETE  /adhesion-pieces/:id} : delete the "id" adhesionPiece.
     *
     * @param id the id of the adhesionPieceDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAdhesionPiece(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete AdhesionPiece : {}", id);
        adhesionPieceService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
