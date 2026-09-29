package cm.gimac.des.homologation.web.rest;

import cm.gimac.des.homologation.repository.ScopeItemRepository;
import cm.gimac.des.homologation.service.ScopeItemService;
import cm.gimac.des.homologation.service.dto.ScopeItemDTO;
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
 * REST controller for managing {@link cm.gimac.des.homologation.domain.ScopeItem}.
 */
@RestController
@RequestMapping("/api/scope-items")
public class ScopeItemResource {

    private static final Logger LOG = LoggerFactory.getLogger(ScopeItemResource.class);

    private static final String ENTITY_NAME = "homologationScopeItem";

    @Value("${jhipster.clientApp.name:homologation}")
    private String applicationName;

    private final ScopeItemService scopeItemService;

    private final ScopeItemRepository scopeItemRepository;

    public ScopeItemResource(ScopeItemService scopeItemService, ScopeItemRepository scopeItemRepository) {
        this.scopeItemService = scopeItemService;
        this.scopeItemRepository = scopeItemRepository;
    }

    /**
     * {@code POST  /scope-items} : Create a new scopeItem.
     *
     * @param scopeItemDTO the scopeItemDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new scopeItemDTO, or with status {@code 400 (Bad Request)} if the scopeItem already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<ScopeItemDTO> createScopeItem(@Valid @RequestBody ScopeItemDTO scopeItemDTO) throws URISyntaxException {
        LOG.debug("REST request to save ScopeItem : {}", scopeItemDTO);
        if (scopeItemDTO.getId() != null) {
            throw new BadRequestAlertException("A new scopeItem cannot already have an ID", ENTITY_NAME, "idexists");
        }
        scopeItemDTO = scopeItemService.save(scopeItemDTO);
        return ResponseEntity.created(new URI("/api/scope-items/" + scopeItemDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, scopeItemDTO.getId().toString()))
            .body(scopeItemDTO);
    }

    /**
     * {@code PUT  /scope-items/:id} : Updates an existing scopeItem.
     *
     * @param id the id of the scopeItemDTO to save.
     * @param scopeItemDTO the scopeItemDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated scopeItemDTO,
     * or with status {@code 400 (Bad Request)} if the scopeItemDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the scopeItemDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ScopeItemDTO> updateScopeItem(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody ScopeItemDTO scopeItemDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update ScopeItem : {}, {}", id, scopeItemDTO);
        if (scopeItemDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, scopeItemDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!scopeItemRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        scopeItemDTO = scopeItemService.update(scopeItemDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, scopeItemDTO.getId().toString()))
            .body(scopeItemDTO);
    }

    /**
     * {@code PATCH  /scope-items/:id} : Partial updates given fields of an existing scopeItem, field will ignore if it is null
     *
     * @param id the id of the scopeItemDTO to save.
     * @param scopeItemDTO the scopeItemDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated scopeItemDTO,
     * or with status {@code 400 (Bad Request)} if the scopeItemDTO is not valid,
     * or with status {@code 404 (Not Found)} if the scopeItemDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the scopeItemDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<ScopeItemDTO> partialUpdateScopeItem(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody ScopeItemDTO scopeItemDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partially update ScopeItem : {}, {}", id, scopeItemDTO);
        if (scopeItemDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, scopeItemDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!scopeItemRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<ScopeItemDTO> result = scopeItemService.partialUpdate(scopeItemDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, scopeItemDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /scope-items} : get all the Scope Items.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Scope Items in body.
     */
    @GetMapping("")
    public List<ScopeItemDTO> getAllScopeItems() {
        LOG.debug("REST request to get all ScopeItems");
        return scopeItemService.findAll();
    }

    /**
     * {@code GET  /scope-items/:id} : get the "id" scopeItem.
     *
     * @param id the id of the scopeItemDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the scopeItemDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ScopeItemDTO> getScopeItem(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get ScopeItem : {}", id);
        Optional<ScopeItemDTO> scopeItemDTO = scopeItemService.findOne(id);
        return ResponseUtil.wrapOrNotFound(scopeItemDTO);
    }

    /**
     * {@code DELETE  /scope-items/:id} : delete the "id" scopeItem.
     *
     * @param id the id of the scopeItemDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteScopeItem(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete ScopeItem : {}", id);
        scopeItemService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
