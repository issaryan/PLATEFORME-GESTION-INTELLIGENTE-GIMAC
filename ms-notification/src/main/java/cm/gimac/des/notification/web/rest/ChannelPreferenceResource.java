package cm.gimac.des.notification.web.rest;

import cm.gimac.des.notification.repository.ChannelPreferenceRepository;
import cm.gimac.des.notification.service.ChannelPreferenceQueryService;
import cm.gimac.des.notification.service.ChannelPreferenceService;
import cm.gimac.des.notification.service.criteria.ChannelPreferenceCriteria;
import cm.gimac.des.notification.service.dto.ChannelPreferenceDTO;
import cm.gimac.des.notification.web.rest.errors.BadRequestAlertException;
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
 * REST controller for managing {@link cm.gimac.des.notification.domain.ChannelPreference}.
 */
@RestController
@RequestMapping("/api/channel-preferences")
public class ChannelPreferenceResource {

    private static final Logger LOG = LoggerFactory.getLogger(ChannelPreferenceResource.class);

    private static final String ENTITY_NAME = "notificationChannelPreference";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ChannelPreferenceService channelPreferenceService;

    private final ChannelPreferenceRepository channelPreferenceRepository;

    private final ChannelPreferenceQueryService channelPreferenceQueryService;

    public ChannelPreferenceResource(
        ChannelPreferenceService channelPreferenceService,
        ChannelPreferenceRepository channelPreferenceRepository,
        ChannelPreferenceQueryService channelPreferenceQueryService
    ) {
        this.channelPreferenceService = channelPreferenceService;
        this.channelPreferenceRepository = channelPreferenceRepository;
        this.channelPreferenceQueryService = channelPreferenceQueryService;
    }

    /**
     * {@code POST  /channel-preferences} : Create a new channelPreference.
     *
     * @param channelPreferenceDTO the channelPreferenceDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new channelPreferenceDTO, or with status {@code 400 (Bad Request)} if the channelPreference has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<ChannelPreferenceDTO> createChannelPreference(@Valid @RequestBody ChannelPreferenceDTO channelPreferenceDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save ChannelPreference : {}", channelPreferenceDTO);
        if (channelPreferenceDTO.getId() != null) {
            throw new BadRequestAlertException("A new channelPreference cannot already have an ID", ENTITY_NAME, "idexists");
        }
        channelPreferenceDTO = channelPreferenceService.save(channelPreferenceDTO);
        return ResponseEntity.created(new URI("/api/channel-preferences/" + channelPreferenceDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, channelPreferenceDTO.getId().toString()))
            .body(channelPreferenceDTO);
    }

    /**
     * {@code PUT  /channel-preferences/:id} : Updates an existing channelPreference.
     *
     * @param id the id of the channelPreferenceDTO to save.
     * @param channelPreferenceDTO the channelPreferenceDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated channelPreferenceDTO,
     * or with status {@code 400 (Bad Request)} if the channelPreferenceDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the channelPreferenceDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ChannelPreferenceDTO> updateChannelPreference(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody ChannelPreferenceDTO channelPreferenceDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update ChannelPreference : {}, {}", id, channelPreferenceDTO);
        if (channelPreferenceDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, channelPreferenceDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!channelPreferenceRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        channelPreferenceDTO = channelPreferenceService.update(channelPreferenceDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, channelPreferenceDTO.getId().toString()))
            .body(channelPreferenceDTO);
    }

    /**
     * {@code PATCH  /channel-preferences/:id} : Partial updates given fields of an existing channelPreference, field will ignore if it is null
     *
     * @param id the id of the channelPreferenceDTO to save.
     * @param channelPreferenceDTO the channelPreferenceDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated channelPreferenceDTO,
     * or with status {@code 400 (Bad Request)} if the channelPreferenceDTO is not valid,
     * or with status {@code 404 (Not Found)} if the channelPreferenceDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the channelPreferenceDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<ChannelPreferenceDTO> partialUpdateChannelPreference(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody ChannelPreferenceDTO channelPreferenceDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update ChannelPreference partially : {}, {}", id, channelPreferenceDTO);
        if (channelPreferenceDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, channelPreferenceDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!channelPreferenceRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<ChannelPreferenceDTO> result = channelPreferenceService.partialUpdate(channelPreferenceDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, channelPreferenceDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /channel-preferences} : get all the channelPreferences.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of channelPreferences in body.
     */
    @GetMapping("")
    public ResponseEntity<List<ChannelPreferenceDTO>> getAllChannelPreferences(
        ChannelPreferenceCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get ChannelPreferences by criteria: {}", criteria);

        Page<ChannelPreferenceDTO> page = channelPreferenceQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /channel-preferences/count} : count all the channelPreferences.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countChannelPreferences(ChannelPreferenceCriteria criteria) {
        LOG.debug("REST request to count ChannelPreferences by criteria: {}", criteria);
        return ResponseEntity.ok().body(channelPreferenceQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /channel-preferences/:id} : get the "id" channelPreference.
     *
     * @param id the id of the channelPreferenceDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the channelPreferenceDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ChannelPreferenceDTO> getChannelPreference(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get ChannelPreference : {}", id);
        Optional<ChannelPreferenceDTO> channelPreferenceDTO = channelPreferenceService.findOne(id);
        return ResponseUtil.wrapOrNotFound(channelPreferenceDTO);
    }

    /**
     * {@code DELETE  /channel-preferences/:id} : delete the "id" channelPreference.
     *
     * @param id the id of the channelPreferenceDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChannelPreference(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete ChannelPreference : {}", id);
        channelPreferenceService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
