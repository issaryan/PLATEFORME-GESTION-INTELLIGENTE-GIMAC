package cm.gimac.des.notification.web.rest;

import cm.gimac.des.notification.repository.AcknowledgementPolicyRepository;
import cm.gimac.des.notification.service.AcknowledgementPolicyService;
import cm.gimac.des.notification.service.dto.AcknowledgementPolicyDTO;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link cm.gimac.des.notification.domain.AcknowledgementPolicy}.
 */
@RestController
@RequestMapping("/api/acknowledgement-policies")
public class AcknowledgementPolicyResource {

    private static final Logger LOG = LoggerFactory.getLogger(AcknowledgementPolicyResource.class);

    private static final String ENTITY_NAME = "notificationAcknowledgementPolicy";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final AcknowledgementPolicyService acknowledgementPolicyService;

    private final AcknowledgementPolicyRepository acknowledgementPolicyRepository;

    public AcknowledgementPolicyResource(
        AcknowledgementPolicyService acknowledgementPolicyService,
        AcknowledgementPolicyRepository acknowledgementPolicyRepository
    ) {
        this.acknowledgementPolicyService = acknowledgementPolicyService;
        this.acknowledgementPolicyRepository = acknowledgementPolicyRepository;
    }

    /**
     * {@code POST  /acknowledgement-policies} : Create a new acknowledgementPolicy.
     *
     * @param acknowledgementPolicyDTO the acknowledgementPolicyDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new acknowledgementPolicyDTO, or with status {@code 400 (Bad Request)} if the acknowledgementPolicy has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<AcknowledgementPolicyDTO> createAcknowledgementPolicy(
        @Valid @RequestBody AcknowledgementPolicyDTO acknowledgementPolicyDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to save AcknowledgementPolicy : {}", acknowledgementPolicyDTO);
        if (acknowledgementPolicyDTO.getId() != null) {
            throw new BadRequestAlertException("A new acknowledgementPolicy cannot already have an ID", ENTITY_NAME, "idexists");
        }
        acknowledgementPolicyDTO = acknowledgementPolicyService.save(acknowledgementPolicyDTO);
        return ResponseEntity.created(new URI("/api/acknowledgement-policies/" + acknowledgementPolicyDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, acknowledgementPolicyDTO.getId().toString()))
            .body(acknowledgementPolicyDTO);
    }

    /**
     * {@code PUT  /acknowledgement-policies/:id} : Updates an existing acknowledgementPolicy.
     *
     * @param id the id of the acknowledgementPolicyDTO to save.
     * @param acknowledgementPolicyDTO the acknowledgementPolicyDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated acknowledgementPolicyDTO,
     * or with status {@code 400 (Bad Request)} if the acknowledgementPolicyDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the acknowledgementPolicyDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<AcknowledgementPolicyDTO> updateAcknowledgementPolicy(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody AcknowledgementPolicyDTO acknowledgementPolicyDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update AcknowledgementPolicy : {}, {}", id, acknowledgementPolicyDTO);
        if (acknowledgementPolicyDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, acknowledgementPolicyDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!acknowledgementPolicyRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        acknowledgementPolicyDTO = acknowledgementPolicyService.update(acknowledgementPolicyDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, acknowledgementPolicyDTO.getId().toString()))
            .body(acknowledgementPolicyDTO);
    }

    /**
     * {@code PATCH  /acknowledgement-policies/:id} : Partial updates given fields of an existing acknowledgementPolicy, field will ignore if it is null
     *
     * @param id the id of the acknowledgementPolicyDTO to save.
     * @param acknowledgementPolicyDTO the acknowledgementPolicyDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated acknowledgementPolicyDTO,
     * or with status {@code 400 (Bad Request)} if the acknowledgementPolicyDTO is not valid,
     * or with status {@code 404 (Not Found)} if the acknowledgementPolicyDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the acknowledgementPolicyDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<AcknowledgementPolicyDTO> partialUpdateAcknowledgementPolicy(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody AcknowledgementPolicyDTO acknowledgementPolicyDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update AcknowledgementPolicy partially : {}, {}", id, acknowledgementPolicyDTO);
        if (acknowledgementPolicyDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, acknowledgementPolicyDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!acknowledgementPolicyRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<AcknowledgementPolicyDTO> result = acknowledgementPolicyService.partialUpdate(acknowledgementPolicyDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, acknowledgementPolicyDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /acknowledgement-policies} : get all the acknowledgementPolicies.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of acknowledgementPolicies in body.
     */
    @GetMapping("")
    public List<AcknowledgementPolicyDTO> getAllAcknowledgementPolicies() {
        LOG.debug("REST request to get all AcknowledgementPolicies");
        return acknowledgementPolicyService.findAll();
    }

    /**
     * {@code GET  /acknowledgement-policies/:id} : get the "id" acknowledgementPolicy.
     *
     * @param id the id of the acknowledgementPolicyDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the acknowledgementPolicyDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AcknowledgementPolicyDTO> getAcknowledgementPolicy(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get AcknowledgementPolicy : {}", id);
        Optional<AcknowledgementPolicyDTO> acknowledgementPolicyDTO = acknowledgementPolicyService.findOne(id);
        return ResponseUtil.wrapOrNotFound(acknowledgementPolicyDTO);
    }

    /**
     * {@code DELETE  /acknowledgement-policies/:id} : delete the "id" acknowledgementPolicy.
     *
     * @param id the id of the acknowledgementPolicyDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAcknowledgementPolicy(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete AcknowledgementPolicy : {}", id);
        acknowledgementPolicyService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
