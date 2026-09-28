package cm.gimac.des.collaboration.web.rest;

import cm.gimac.des.collaboration.repository.RoomRetentionPolicyRepository;
import cm.gimac.des.collaboration.service.RoomRetentionPolicyService;
import cm.gimac.des.collaboration.service.dto.RoomRetentionPolicyDTO;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link cm.gimac.des.collaboration.domain.RoomRetentionPolicy}.
 */
@RestController
@RequestMapping("/api/room-retention-policies")
public class RoomRetentionPolicyResource {

    private static final Logger LOG = LoggerFactory.getLogger(RoomRetentionPolicyResource.class);

    private static final String ENTITY_NAME = "collaborationRoomRetentionPolicy";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final RoomRetentionPolicyService roomRetentionPolicyService;

    private final RoomRetentionPolicyRepository roomRetentionPolicyRepository;

    public RoomRetentionPolicyResource(
        RoomRetentionPolicyService roomRetentionPolicyService,
        RoomRetentionPolicyRepository roomRetentionPolicyRepository
    ) {
        this.roomRetentionPolicyService = roomRetentionPolicyService;
        this.roomRetentionPolicyRepository = roomRetentionPolicyRepository;
    }

    /**
     * {@code POST  /room-retention-policies} : Create a new roomRetentionPolicy.
     *
     * @param roomRetentionPolicyDTO the roomRetentionPolicyDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new roomRetentionPolicyDTO, or with status {@code 400 (Bad Request)} if the roomRetentionPolicy has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<RoomRetentionPolicyDTO> createRoomRetentionPolicy(
        @Valid @RequestBody RoomRetentionPolicyDTO roomRetentionPolicyDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to save RoomRetentionPolicy : {}", roomRetentionPolicyDTO);
        if (roomRetentionPolicyDTO.getId() != null) {
            throw new BadRequestAlertException("A new roomRetentionPolicy cannot already have an ID", ENTITY_NAME, "idexists");
        }
        roomRetentionPolicyDTO = roomRetentionPolicyService.save(roomRetentionPolicyDTO);
        return ResponseEntity.created(new URI("/api/room-retention-policies/" + roomRetentionPolicyDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, roomRetentionPolicyDTO.getId().toString()))
            .body(roomRetentionPolicyDTO);
    }

    /**
     * {@code PUT  /room-retention-policies/:id} : Updates an existing roomRetentionPolicy.
     *
     * @param id the id of the roomRetentionPolicyDTO to save.
     * @param roomRetentionPolicyDTO the roomRetentionPolicyDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated roomRetentionPolicyDTO,
     * or with status {@code 400 (Bad Request)} if the roomRetentionPolicyDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the roomRetentionPolicyDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<RoomRetentionPolicyDTO> updateRoomRetentionPolicy(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody RoomRetentionPolicyDTO roomRetentionPolicyDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update RoomRetentionPolicy : {}, {}", id, roomRetentionPolicyDTO);
        if (roomRetentionPolicyDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, roomRetentionPolicyDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!roomRetentionPolicyRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        roomRetentionPolicyDTO = roomRetentionPolicyService.update(roomRetentionPolicyDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, roomRetentionPolicyDTO.getId().toString()))
            .body(roomRetentionPolicyDTO);
    }

    /**
     * {@code PATCH  /room-retention-policies/:id} : Partial updates given fields of an existing roomRetentionPolicy, field will ignore if it is null
     *
     * @param id the id of the roomRetentionPolicyDTO to save.
     * @param roomRetentionPolicyDTO the roomRetentionPolicyDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated roomRetentionPolicyDTO,
     * or with status {@code 400 (Bad Request)} if the roomRetentionPolicyDTO is not valid,
     * or with status {@code 404 (Not Found)} if the roomRetentionPolicyDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the roomRetentionPolicyDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<RoomRetentionPolicyDTO> partialUpdateRoomRetentionPolicy(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody RoomRetentionPolicyDTO roomRetentionPolicyDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update RoomRetentionPolicy partially : {}, {}", id, roomRetentionPolicyDTO);
        if (roomRetentionPolicyDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, roomRetentionPolicyDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!roomRetentionPolicyRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<RoomRetentionPolicyDTO> result = roomRetentionPolicyService.partialUpdate(roomRetentionPolicyDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, roomRetentionPolicyDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /room-retention-policies} : get all the roomRetentionPolicies.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of roomRetentionPolicies in body.
     */
    @GetMapping("")
    public List<RoomRetentionPolicyDTO> getAllRoomRetentionPolicies() {
        LOG.debug("REST request to get all RoomRetentionPolicies");
        return roomRetentionPolicyService.findAll();
    }

    /**
     * {@code GET  /room-retention-policies/:id} : get the "id" roomRetentionPolicy.
     *
     * @param id the id of the roomRetentionPolicyDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the roomRetentionPolicyDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RoomRetentionPolicyDTO> getRoomRetentionPolicy(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get RoomRetentionPolicy : {}", id);
        Optional<RoomRetentionPolicyDTO> roomRetentionPolicyDTO = roomRetentionPolicyService.findOne(id);
        return ResponseUtil.wrapOrNotFound(roomRetentionPolicyDTO);
    }

    /**
     * {@code DELETE  /room-retention-policies/:id} : delete the "id" roomRetentionPolicy.
     *
     * @param id the id of the roomRetentionPolicyDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoomRetentionPolicy(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete RoomRetentionPolicy : {}", id);
        roomRetentionPolicyService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
