package cm.gimac.des.pilotage.web.rest;

import cm.gimac.des.pilotage.repository.ActionSheetRepository;
import cm.gimac.des.pilotage.service.ActionSheetQueryService;
import cm.gimac.des.pilotage.service.ActionSheetService;
import cm.gimac.des.pilotage.service.criteria.ActionSheetCriteria;
import cm.gimac.des.pilotage.service.dto.ActionSheetDTO;
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
 * REST controller for managing {@link cm.gimac.des.pilotage.domain.ActionSheet}.
 */
@RestController
@RequestMapping("/api/action-sheets")
public class ActionSheetResource {

    private static final Logger LOG = LoggerFactory.getLogger(ActionSheetResource.class);

    private static final String ENTITY_NAME = "pilotageActionSheet";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ActionSheetService actionSheetService;

    private final ActionSheetRepository actionSheetRepository;

    private final ActionSheetQueryService actionSheetQueryService;

    public ActionSheetResource(
        ActionSheetService actionSheetService,
        ActionSheetRepository actionSheetRepository,
        ActionSheetQueryService actionSheetQueryService
    ) {
        this.actionSheetService = actionSheetService;
        this.actionSheetRepository = actionSheetRepository;
        this.actionSheetQueryService = actionSheetQueryService;
    }

    /**
     * {@code POST  /action-sheets} : Create a new actionSheet.
     *
     * @param actionSheetDTO the actionSheetDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new actionSheetDTO, or with status {@code 400 (Bad Request)} if the actionSheet has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<ActionSheetDTO> createActionSheet(@Valid @RequestBody ActionSheetDTO actionSheetDTO) throws URISyntaxException {
        LOG.debug("REST request to save ActionSheet : {}", actionSheetDTO);
        if (actionSheetDTO.getId() != null) {
            throw new BadRequestAlertException("A new actionSheet cannot already have an ID", ENTITY_NAME, "idexists");
        }
        actionSheetDTO = actionSheetService.save(actionSheetDTO);
        return ResponseEntity.created(new URI("/api/action-sheets/" + actionSheetDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, actionSheetDTO.getId().toString()))
            .body(actionSheetDTO);
    }

    /**
     * {@code PUT  /action-sheets/:id} : Updates an existing actionSheet.
     *
     * @param id the id of the actionSheetDTO to save.
     * @param actionSheetDTO the actionSheetDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated actionSheetDTO,
     * or with status {@code 400 (Bad Request)} if the actionSheetDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the actionSheetDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ActionSheetDTO> updateActionSheet(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody ActionSheetDTO actionSheetDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update ActionSheet : {}, {}", id, actionSheetDTO);
        if (actionSheetDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, actionSheetDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!actionSheetRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        actionSheetDTO = actionSheetService.update(actionSheetDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, actionSheetDTO.getId().toString()))
            .body(actionSheetDTO);
    }

    /**
     * {@code PATCH  /action-sheets/:id} : Partial updates given fields of an existing actionSheet, field will ignore if it is null
     *
     * @param id the id of the actionSheetDTO to save.
     * @param actionSheetDTO the actionSheetDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated actionSheetDTO,
     * or with status {@code 400 (Bad Request)} if the actionSheetDTO is not valid,
     * or with status {@code 404 (Not Found)} if the actionSheetDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the actionSheetDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<ActionSheetDTO> partialUpdateActionSheet(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody ActionSheetDTO actionSheetDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update ActionSheet partially : {}, {}", id, actionSheetDTO);
        if (actionSheetDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, actionSheetDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!actionSheetRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<ActionSheetDTO> result = actionSheetService.partialUpdate(actionSheetDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, actionSheetDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /action-sheets} : get all the actionSheets.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of actionSheets in body.
     */
    @GetMapping("")
    public ResponseEntity<List<ActionSheetDTO>> getAllActionSheets(
        ActionSheetCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get ActionSheets by criteria: {}", criteria);

        Page<ActionSheetDTO> page = actionSheetQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /action-sheets/count} : count all the actionSheets.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countActionSheets(ActionSheetCriteria criteria) {
        LOG.debug("REST request to count ActionSheets by criteria: {}", criteria);
        return ResponseEntity.ok().body(actionSheetQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /action-sheets/:id} : get the "id" actionSheet.
     *
     * @param id the id of the actionSheetDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the actionSheetDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ActionSheetDTO> getActionSheet(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get ActionSheet : {}", id);
        Optional<ActionSheetDTO> actionSheetDTO = actionSheetService.findOne(id);
        return ResponseUtil.wrapOrNotFound(actionSheetDTO);
    }

    /**
     * {@code DELETE  /action-sheets/:id} : delete the "id" actionSheet.
     *
     * @param id the id of the actionSheetDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteActionSheet(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete ActionSheet : {}", id);
        actionSheetService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
