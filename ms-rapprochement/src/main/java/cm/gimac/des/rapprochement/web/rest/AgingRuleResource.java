package cm.gimac.des.rapprochement.web.rest;

import cm.gimac.des.rapprochement.repository.AgingRuleRepository;
import cm.gimac.des.rapprochement.service.AgingRuleService;
import cm.gimac.des.rapprochement.service.dto.AgingRuleDTO;
import cm.gimac.des.rapprochement.web.rest.errors.BadRequestAlertException;
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
 * REST controller for managing {@link cm.gimac.des.rapprochement.domain.AgingRule}.
 */
@RestController
@RequestMapping("/api/aging-rules")
public class AgingRuleResource {

    private static final Logger LOG = LoggerFactory.getLogger(AgingRuleResource.class);

    private static final String ENTITY_NAME = "rapprochementAgingRule";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final AgingRuleService agingRuleService;

    private final AgingRuleRepository agingRuleRepository;

    public AgingRuleResource(AgingRuleService agingRuleService, AgingRuleRepository agingRuleRepository) {
        this.agingRuleService = agingRuleService;
        this.agingRuleRepository = agingRuleRepository;
    }

    /**
     * {@code POST  /aging-rules} : Create a new agingRule.
     *
     * @param agingRuleDTO the agingRuleDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new agingRuleDTO, or with status {@code 400 (Bad Request)} if the agingRule has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<AgingRuleDTO> createAgingRule(@Valid @RequestBody AgingRuleDTO agingRuleDTO) throws URISyntaxException {
        LOG.debug("REST request to save AgingRule : {}", agingRuleDTO);
        if (agingRuleDTO.getId() != null) {
            throw new BadRequestAlertException("A new agingRule cannot already have an ID", ENTITY_NAME, "idexists");
        }
        agingRuleDTO = agingRuleService.save(agingRuleDTO);
        return ResponseEntity.created(new URI("/api/aging-rules/" + agingRuleDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, agingRuleDTO.getId().toString()))
            .body(agingRuleDTO);
    }

    /**
     * {@code PUT  /aging-rules/:id} : Updates an existing agingRule.
     *
     * @param id the id of the agingRuleDTO to save.
     * @param agingRuleDTO the agingRuleDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated agingRuleDTO,
     * or with status {@code 400 (Bad Request)} if the agingRuleDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the agingRuleDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<AgingRuleDTO> updateAgingRule(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody AgingRuleDTO agingRuleDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update AgingRule : {}, {}", id, agingRuleDTO);
        if (agingRuleDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, agingRuleDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!agingRuleRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        agingRuleDTO = agingRuleService.update(agingRuleDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, agingRuleDTO.getId().toString()))
            .body(agingRuleDTO);
    }

    /**
     * {@code PATCH  /aging-rules/:id} : Partial updates given fields of an existing agingRule, field will ignore if it is null
     *
     * @param id the id of the agingRuleDTO to save.
     * @param agingRuleDTO the agingRuleDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated agingRuleDTO,
     * or with status {@code 400 (Bad Request)} if the agingRuleDTO is not valid,
     * or with status {@code 404 (Not Found)} if the agingRuleDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the agingRuleDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<AgingRuleDTO> partialUpdateAgingRule(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody AgingRuleDTO agingRuleDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update AgingRule partially : {}, {}", id, agingRuleDTO);
        if (agingRuleDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, agingRuleDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!agingRuleRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<AgingRuleDTO> result = agingRuleService.partialUpdate(agingRuleDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, agingRuleDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /aging-rules} : get all the agingRules.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of agingRules in body.
     */
    @GetMapping("")
    public List<AgingRuleDTO> getAllAgingRules() {
        LOG.debug("REST request to get all AgingRules");
        return agingRuleService.findAll();
    }

    /**
     * {@code GET  /aging-rules/:id} : get the "id" agingRule.
     *
     * @param id the id of the agingRuleDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the agingRuleDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AgingRuleDTO> getAgingRule(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get AgingRule : {}", id);
        Optional<AgingRuleDTO> agingRuleDTO = agingRuleService.findOne(id);
        return ResponseUtil.wrapOrNotFound(agingRuleDTO);
    }

    /**
     * {@code DELETE  /aging-rules/:id} : delete the "id" agingRule.
     *
     * @param id the id of the agingRuleDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAgingRule(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete AgingRule : {}", id);
        agingRuleService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
