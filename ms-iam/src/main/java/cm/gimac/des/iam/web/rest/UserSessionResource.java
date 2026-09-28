package cm.gimac.des.iam.web.rest;

import cm.gimac.des.iam.repository.UserSessionRepository;
import cm.gimac.des.iam.service.UserSessionQueryService;
import cm.gimac.des.iam.service.UserSessionService;
import cm.gimac.des.iam.service.criteria.UserSessionCriteria;
import cm.gimac.des.iam.service.dto.UserSessionDTO;
import cm.gimac.des.iam.web.rest.errors.BadRequestAlertException;
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
 * REST controller for managing {@link cm.gimac.des.iam.domain.UserSession}.
 */
@RestController
@RequestMapping("/api/user-sessions")
public class UserSessionResource {

    private static final Logger LOG = LoggerFactory.getLogger(UserSessionResource.class);

    private static final String ENTITY_NAME = "iamUserSession";

    @Value("${jhipster.clientApp.name:iam}")
    private String applicationName;

    private final UserSessionService userSessionService;

    private final UserSessionRepository userSessionRepository;

    private final UserSessionQueryService userSessionQueryService;

    public UserSessionResource(
        UserSessionService userSessionService,
        UserSessionRepository userSessionRepository,
        UserSessionQueryService userSessionQueryService
    ) {
        this.userSessionService = userSessionService;
        this.userSessionRepository = userSessionRepository;
        this.userSessionQueryService = userSessionQueryService;
    }

    /**
     * {@code POST  /user-sessions} : Create a new userSession.
     *
     * @param userSessionDTO the userSessionDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new userSessionDTO, or with status {@code 400 (Bad Request)} if the userSession already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<UserSessionDTO> createUserSession(@Valid @RequestBody UserSessionDTO userSessionDTO) throws URISyntaxException {
        LOG.debug("REST request to save UserSession : {}", userSessionDTO);
        if (userSessionDTO.getId() != null) {
            throw new BadRequestAlertException("A new userSession cannot already have an ID", ENTITY_NAME, "idexists");
        }
        userSessionDTO = userSessionService.save(userSessionDTO);
        return ResponseEntity.created(new URI("/api/user-sessions/" + userSessionDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, userSessionDTO.getId().toString()))
            .body(userSessionDTO);
    }

    /**
     * {@code PUT  /user-sessions/:id} : Updates an existing userSession.
     *
     * @param id the id of the userSessionDTO to save.
     * @param userSessionDTO the userSessionDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated userSessionDTO,
     * or with status {@code 400 (Bad Request)} if the userSessionDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the userSessionDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserSessionDTO> updateUserSession(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody UserSessionDTO userSessionDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update UserSession : {}, {}", id, userSessionDTO);
        if (userSessionDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, userSessionDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!userSessionRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        userSessionDTO = userSessionService.update(userSessionDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, userSessionDTO.getId().toString()))
            .body(userSessionDTO);
    }

    /**
     * {@code PATCH  /user-sessions/:id} : Partial updates given fields of an existing userSession, field will ignore if it is null
     *
     * @param id the id of the userSessionDTO to save.
     * @param userSessionDTO the userSessionDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated userSessionDTO,
     * or with status {@code 400 (Bad Request)} if the userSessionDTO is not valid,
     * or with status {@code 404 (Not Found)} if the userSessionDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the userSessionDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<UserSessionDTO> partialUpdateUserSession(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody UserSessionDTO userSessionDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partially update UserSession : {}, {}", id, userSessionDTO);
        if (userSessionDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, userSessionDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!userSessionRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<UserSessionDTO> result = userSessionService.partialUpdate(userSessionDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, userSessionDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /user-sessions} : get all the User Sessions.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of User Sessions in body.
     */
    @GetMapping("")
    public ResponseEntity<List<UserSessionDTO>> getAllUserSessions(
        UserSessionCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get UserSessions by criteria: {}", criteria);

        Page<UserSessionDTO> page = userSessionQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /user-sessions/count} : count all the userSessions.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countUserSessions(UserSessionCriteria criteria) {
        LOG.debug("REST request to count UserSessions by criteria: {}", criteria);
        return ResponseEntity.ok().body(userSessionQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /user-sessions/:id} : get the "id" userSession.
     *
     * @param id the id of the userSessionDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the userSessionDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserSessionDTO> getUserSession(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get UserSession : {}", id);
        Optional<UserSessionDTO> userSessionDTO = userSessionService.findOne(id);
        return ResponseUtil.wrapOrNotFound(userSessionDTO);
    }

    /**
     * {@code DELETE  /user-sessions/:id} : delete the "id" userSession.
     *
     * @param id the id of the userSessionDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUserSession(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete UserSession : {}", id);
        userSessionService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
