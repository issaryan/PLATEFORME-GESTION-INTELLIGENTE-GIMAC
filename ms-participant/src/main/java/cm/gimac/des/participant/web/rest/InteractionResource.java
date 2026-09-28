package cm.gimac.des.participant.web.rest;

import cm.gimac.des.participant.repository.InteractionRepository;
import cm.gimac.des.participant.service.InteractionQueryService;
import cm.gimac.des.participant.service.InteractionService;
import cm.gimac.des.participant.service.criteria.InteractionCriteria;
import cm.gimac.des.participant.service.dto.InteractionDTO;
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
 * REST controller for managing {@link cm.gimac.des.participant.domain.Interaction}.
 */
@RestController
@RequestMapping("/api/interactions")
public class InteractionResource {

    private static final Logger LOG = LoggerFactory.getLogger(InteractionResource.class);

    private static final String ENTITY_NAME = "participantInteraction";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final InteractionService interactionService;

    private final InteractionRepository interactionRepository;

    private final InteractionQueryService interactionQueryService;

    public InteractionResource(
        InteractionService interactionService,
        InteractionRepository interactionRepository,
        InteractionQueryService interactionQueryService
    ) {
        this.interactionService = interactionService;
        this.interactionRepository = interactionRepository;
        this.interactionQueryService = interactionQueryService;
    }

    /**
     * {@code POST  /interactions} : Create a new interaction.
     *
     * @param interactionDTO the interactionDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new interactionDTO, or with status {@code 400 (Bad Request)} if the interaction has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<InteractionDTO> createInteraction(@Valid @RequestBody InteractionDTO interactionDTO) throws URISyntaxException {
        LOG.debug("REST request to save Interaction : {}", interactionDTO);
        if (interactionDTO.getId() != null) {
            throw new BadRequestAlertException("A new interaction cannot already have an ID", ENTITY_NAME, "idexists");
        }
        interactionDTO = interactionService.save(interactionDTO);
        return ResponseEntity.created(new URI("/api/interactions/" + interactionDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, interactionDTO.getId().toString()))
            .body(interactionDTO);
    }

    /**
     * {@code PUT  /interactions/:id} : Updates an existing interaction.
     *
     * @param id the id of the interactionDTO to save.
     * @param interactionDTO the interactionDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated interactionDTO,
     * or with status {@code 400 (Bad Request)} if the interactionDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the interactionDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<InteractionDTO> updateInteraction(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody InteractionDTO interactionDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Interaction : {}, {}", id, interactionDTO);
        if (interactionDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, interactionDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!interactionRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        interactionDTO = interactionService.update(interactionDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, interactionDTO.getId().toString()))
            .body(interactionDTO);
    }

    /**
     * {@code PATCH  /interactions/:id} : Partial updates given fields of an existing interaction, field will ignore if it is null
     *
     * @param id the id of the interactionDTO to save.
     * @param interactionDTO the interactionDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated interactionDTO,
     * or with status {@code 400 (Bad Request)} if the interactionDTO is not valid,
     * or with status {@code 404 (Not Found)} if the interactionDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the interactionDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<InteractionDTO> partialUpdateInteraction(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody InteractionDTO interactionDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Interaction partially : {}, {}", id, interactionDTO);
        if (interactionDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, interactionDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!interactionRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<InteractionDTO> result = interactionService.partialUpdate(interactionDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, interactionDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /interactions} : get all the interactions.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of interactions in body.
     */
    @GetMapping("")
    public ResponseEntity<List<InteractionDTO>> getAllInteractions(
        InteractionCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get Interactions by criteria: {}", criteria);

        Page<InteractionDTO> page = interactionQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /interactions/count} : count all the interactions.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countInteractions(InteractionCriteria criteria) {
        LOG.debug("REST request to count Interactions by criteria: {}", criteria);
        return ResponseEntity.ok().body(interactionQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /interactions/:id} : get the "id" interaction.
     *
     * @param id the id of the interactionDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the interactionDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<InteractionDTO> getInteraction(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get Interaction : {}", id);
        Optional<InteractionDTO> interactionDTO = interactionService.findOne(id);
        return ResponseUtil.wrapOrNotFound(interactionDTO);
    }

    /**
     * {@code DELETE  /interactions/:id} : delete the "id" interaction.
     *
     * @param id the id of the interactionDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInteraction(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete Interaction : {}", id);
        interactionService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
