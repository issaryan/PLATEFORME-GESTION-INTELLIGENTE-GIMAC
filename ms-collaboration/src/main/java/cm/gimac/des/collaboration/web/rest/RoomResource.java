package cm.gimac.des.collaboration.web.rest;

import cm.gimac.des.collaboration.repository.RoomRepository;
import cm.gimac.des.collaboration.service.RoomQueryService;
import cm.gimac.des.collaboration.service.RoomService;
import cm.gimac.des.collaboration.service.criteria.RoomCriteria;
import cm.gimac.des.collaboration.service.dto.RoomDTO;
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
 * REST controller for managing {@link cm.gimac.des.collaboration.domain.Room}.
 */
@RestController
@RequestMapping("/api/rooms")
public class RoomResource {

    private static final Logger LOG = LoggerFactory.getLogger(RoomResource.class);

    private static final String ENTITY_NAME = "collaborationRoom";

    @Value("${jhipster.clientApp.name:collaboration}")
    private String applicationName;

    private final RoomService roomService;

    private final RoomRepository roomRepository;

    private final RoomQueryService roomQueryService;

    public RoomResource(RoomService roomService, RoomRepository roomRepository, RoomQueryService roomQueryService) {
        this.roomService = roomService;
        this.roomRepository = roomRepository;
        this.roomQueryService = roomQueryService;
    }

    /**
     * {@code POST  /rooms} : Create a new room.
     *
     * @param roomDTO the roomDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new roomDTO, or with status {@code 400 (Bad Request)} if the room already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<RoomDTO> createRoom(@Valid @RequestBody RoomDTO roomDTO) throws URISyntaxException {
        LOG.debug("REST request to save Room : {}", roomDTO);
        if (roomDTO.getId() != null) {
            throw new BadRequestAlertException("A new room cannot already have an ID", ENTITY_NAME, "idexists");
        }
        roomDTO = roomService.save(roomDTO);
        return ResponseEntity.created(new URI("/api/rooms/" + roomDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, roomDTO.getId().toString()))
            .body(roomDTO);
    }

    /**
     * {@code PUT  /rooms/:id} : Updates an existing room.
     *
     * @param id the id of the roomDTO to save.
     * @param roomDTO the roomDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated roomDTO,
     * or with status {@code 400 (Bad Request)} if the roomDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the roomDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<RoomDTO> updateRoom(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody RoomDTO roomDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Room : {}, {}", id, roomDTO);
        if (roomDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, roomDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!roomRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        roomDTO = roomService.update(roomDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, roomDTO.getId().toString()))
            .body(roomDTO);
    }

    /**
     * {@code PATCH  /rooms/:id} : Partial updates given fields of an existing room, field will ignore if it is null
     *
     * @param id the id of the roomDTO to save.
     * @param roomDTO the roomDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated roomDTO,
     * or with status {@code 400 (Bad Request)} if the roomDTO is not valid,
     * or with status {@code 404 (Not Found)} if the roomDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the roomDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<RoomDTO> partialUpdateRoom(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody RoomDTO roomDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partially update Room : {}, {}", id, roomDTO);
        if (roomDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, roomDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!roomRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<RoomDTO> result = roomService.partialUpdate(roomDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, roomDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /rooms} : get all the Rooms.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Rooms in body.
     */
    @GetMapping("")
    public ResponseEntity<List<RoomDTO>> getAllRooms(
        RoomCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get Rooms by criteria: {}", criteria);

        Page<RoomDTO> page = roomQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /rooms/count} : count all the rooms.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countRooms(RoomCriteria criteria) {
        LOG.debug("REST request to count Rooms by criteria: {}", criteria);
        return ResponseEntity.ok().body(roomQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /rooms/:id} : get the "id" room.
     *
     * @param id the id of the roomDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the roomDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RoomDTO> getRoom(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get Room : {}", id);
        Optional<RoomDTO> roomDTO = roomService.findOne(id);
        return ResponseUtil.wrapOrNotFound(roomDTO);
    }

    /**
     * {@code DELETE  /rooms/:id} : delete the "id" room.
     *
     * @param id the id of the roomDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete Room : {}", id);
        roomService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
