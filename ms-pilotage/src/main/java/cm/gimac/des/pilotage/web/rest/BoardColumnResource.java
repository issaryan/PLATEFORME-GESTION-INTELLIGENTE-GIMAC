package cm.gimac.des.pilotage.web.rest;

import cm.gimac.des.pilotage.repository.BoardColumnRepository;
import cm.gimac.des.pilotage.service.BoardColumnService;
import cm.gimac.des.pilotage.service.dto.BoardColumnDTO;
import cm.gimac.des.pilotage.web.rest.errors.BadRequestAlertException;
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
 * REST controller for managing {@link cm.gimac.des.pilotage.domain.BoardColumn}.
 */
@RestController
@RequestMapping("/api/board-columns")
public class BoardColumnResource {

    private static final Logger LOG = LoggerFactory.getLogger(BoardColumnResource.class);

    private static final String ENTITY_NAME = "pilotageBoardColumn";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final BoardColumnService boardColumnService;

    private final BoardColumnRepository boardColumnRepository;

    public BoardColumnResource(BoardColumnService boardColumnService, BoardColumnRepository boardColumnRepository) {
        this.boardColumnService = boardColumnService;
        this.boardColumnRepository = boardColumnRepository;
    }

    /**
     * {@code POST  /board-columns} : Create a new boardColumn.
     *
     * @param boardColumnDTO the boardColumnDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new boardColumnDTO, or with status {@code 400 (Bad Request)} if the boardColumn has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<BoardColumnDTO> createBoardColumn(@Valid @RequestBody BoardColumnDTO boardColumnDTO) throws URISyntaxException {
        LOG.debug("REST request to save BoardColumn : {}", boardColumnDTO);
        if (boardColumnDTO.getId() != null) {
            throw new BadRequestAlertException("A new boardColumn cannot already have an ID", ENTITY_NAME, "idexists");
        }
        boardColumnDTO = boardColumnService.save(boardColumnDTO);
        return ResponseEntity.created(new URI("/api/board-columns/" + boardColumnDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, boardColumnDTO.getId().toString()))
            .body(boardColumnDTO);
    }

    /**
     * {@code PUT  /board-columns/:id} : Updates an existing boardColumn.
     *
     * @param id the id of the boardColumnDTO to save.
     * @param boardColumnDTO the boardColumnDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated boardColumnDTO,
     * or with status {@code 400 (Bad Request)} if the boardColumnDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the boardColumnDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<BoardColumnDTO> updateBoardColumn(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody BoardColumnDTO boardColumnDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update BoardColumn : {}, {}", id, boardColumnDTO);
        if (boardColumnDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, boardColumnDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!boardColumnRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        boardColumnDTO = boardColumnService.update(boardColumnDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, boardColumnDTO.getId().toString()))
            .body(boardColumnDTO);
    }

    /**
     * {@code PATCH  /board-columns/:id} : Partial updates given fields of an existing boardColumn, field will ignore if it is null
     *
     * @param id the id of the boardColumnDTO to save.
     * @param boardColumnDTO the boardColumnDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated boardColumnDTO,
     * or with status {@code 400 (Bad Request)} if the boardColumnDTO is not valid,
     * or with status {@code 404 (Not Found)} if the boardColumnDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the boardColumnDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<BoardColumnDTO> partialUpdateBoardColumn(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody BoardColumnDTO boardColumnDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update BoardColumn partially : {}, {}", id, boardColumnDTO);
        if (boardColumnDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, boardColumnDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!boardColumnRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<BoardColumnDTO> result = boardColumnService.partialUpdate(boardColumnDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, boardColumnDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /board-columns} : get all the boardColumns.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of boardColumns in body.
     */
    @GetMapping("")
    public List<BoardColumnDTO> getAllBoardColumns(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all BoardColumns");
        return boardColumnService.findAll();
    }

    /**
     * {@code GET  /board-columns/:id} : get the "id" boardColumn.
     *
     * @param id the id of the boardColumnDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the boardColumnDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<BoardColumnDTO> getBoardColumn(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get BoardColumn : {}", id);
        Optional<BoardColumnDTO> boardColumnDTO = boardColumnService.findOne(id);
        return ResponseUtil.wrapOrNotFound(boardColumnDTO);
    }

    /**
     * {@code DELETE  /board-columns/:id} : delete the "id" boardColumn.
     *
     * @param id the id of the boardColumnDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBoardColumn(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete BoardColumn : {}", id);
        boardColumnService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
