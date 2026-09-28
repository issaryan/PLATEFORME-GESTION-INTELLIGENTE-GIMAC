package cm.gimac.des.collaboration.web.rest;

import cm.gimac.des.collaboration.repository.RoomMemberRepository;
import cm.gimac.des.collaboration.service.RoomMemberQueryService;
import cm.gimac.des.collaboration.service.RoomMemberService;
import cm.gimac.des.collaboration.service.criteria.RoomMemberCriteria;
import cm.gimac.des.collaboration.service.dto.RoomMemberDTO;
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
 * REST controller for managing {@link cm.gimac.des.collaboration.domain.RoomMember}.
 */
@RestController
@RequestMapping("/api/room-members")
public class RoomMemberResource {

    private static final Logger LOG = LoggerFactory.getLogger(RoomMemberResource.class);

    private static final String ENTITY_NAME = "collaborationRoomMember";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final RoomMemberService roomMemberService;

    private final RoomMemberRepository roomMemberRepository;

    private final RoomMemberQueryService roomMemberQueryService;

    public RoomMemberResource(
        RoomMemberService roomMemberService,
        RoomMemberRepository roomMemberRepository,
        RoomMemberQueryService roomMemberQueryService
    ) {
        this.roomMemberService = roomMemberService;
        this.roomMemberRepository = roomMemberRepository;
        this.roomMemberQueryService = roomMemberQueryService;
    }

    /**
     * {@code POST  /room-members} : Create a new roomMember.
     *
     * @param roomMemberDTO the roomMemberDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new roomMemberDTO, or with status {@code 400 (Bad Request)} if the roomMember has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<RoomMemberDTO> createRoomMember(@Valid @RequestBody RoomMemberDTO roomMemberDTO) throws URISyntaxException {
        LOG.debug("REST request to save RoomMember : {}", roomMemberDTO);
        if (roomMemberDTO.getId() != null) {
            throw new BadRequestAlertException("A new roomMember cannot already have an ID", ENTITY_NAME, "idexists");
        }
        roomMemberDTO = roomMemberService.save(roomMemberDTO);
        return ResponseEntity.created(new URI("/api/room-members/" + roomMemberDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, roomMemberDTO.getId().toString()))
            .body(roomMemberDTO);
    }

    /**
     * {@code PUT  /room-members/:id} : Updates an existing roomMember.
     *
     * @param id the id of the roomMemberDTO to save.
     * @param roomMemberDTO the roomMemberDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated roomMemberDTO,
     * or with status {@code 400 (Bad Request)} if the roomMemberDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the roomMemberDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<RoomMemberDTO> updateRoomMember(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody RoomMemberDTO roomMemberDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update RoomMember : {}, {}", id, roomMemberDTO);
        if (roomMemberDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, roomMemberDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!roomMemberRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        roomMemberDTO = roomMemberService.update(roomMemberDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, roomMemberDTO.getId().toString()))
            .body(roomMemberDTO);
    }

    /**
     * {@code PATCH  /room-members/:id} : Partial updates given fields of an existing roomMember, field will ignore if it is null
     *
     * @param id the id of the roomMemberDTO to save.
     * @param roomMemberDTO the roomMemberDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated roomMemberDTO,
     * or with status {@code 400 (Bad Request)} if the roomMemberDTO is not valid,
     * or with status {@code 404 (Not Found)} if the roomMemberDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the roomMemberDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<RoomMemberDTO> partialUpdateRoomMember(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody RoomMemberDTO roomMemberDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update RoomMember partially : {}, {}", id, roomMemberDTO);
        if (roomMemberDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, roomMemberDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!roomMemberRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<RoomMemberDTO> result = roomMemberService.partialUpdate(roomMemberDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, roomMemberDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /room-members} : get all the roomMembers.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of roomMembers in body.
     */
    @GetMapping("")
    public ResponseEntity<List<RoomMemberDTO>> getAllRoomMembers(
        RoomMemberCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get RoomMembers by criteria: {}", criteria);

        Page<RoomMemberDTO> page = roomMemberQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /room-members/count} : count all the roomMembers.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countRoomMembers(RoomMemberCriteria criteria) {
        LOG.debug("REST request to count RoomMembers by criteria: {}", criteria);
        return ResponseEntity.ok().body(roomMemberQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /room-members/:id} : get the "id" roomMember.
     *
     * @param id the id of the roomMemberDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the roomMemberDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RoomMemberDTO> getRoomMember(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get RoomMember : {}", id);
        Optional<RoomMemberDTO> roomMemberDTO = roomMemberService.findOne(id);
        return ResponseUtil.wrapOrNotFound(roomMemberDTO);
    }

    /**
     * {@code DELETE  /room-members/:id} : delete the "id" roomMember.
     *
     * @param id the id of the roomMemberDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoomMember(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete RoomMember : {}", id);
        roomMemberService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
