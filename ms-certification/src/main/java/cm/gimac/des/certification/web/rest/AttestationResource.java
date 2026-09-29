package cm.gimac.des.certification.web.rest;

import cm.gimac.des.certification.repository.AttestationRepository;
import cm.gimac.des.certification.service.AttestationService;
import cm.gimac.des.certification.service.dto.AttestationDTO;
import cm.gimac.des.certification.web.rest.errors.BadRequestAlertException;
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
 * REST controller for managing {@link cm.gimac.des.certification.domain.Attestation}.
 */
@RestController
@RequestMapping("/api/attestations")
public class AttestationResource {

    private static final Logger LOG = LoggerFactory.getLogger(AttestationResource.class);

    private static final String ENTITY_NAME = "certificationAttestation";

    @Value("${jhipster.clientApp.name:certification}")
    private String applicationName;

    private final AttestationService attestationService;

    private final AttestationRepository attestationRepository;

    public AttestationResource(AttestationService attestationService, AttestationRepository attestationRepository) {
        this.attestationService = attestationService;
        this.attestationRepository = attestationRepository;
    }

    /**
     * {@code POST  /attestations} : Create a new attestation.
     *
     * @param attestationDTO the attestationDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new attestationDTO, or with status {@code 400 (Bad Request)} if the attestation already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<AttestationDTO> createAttestation(@Valid @RequestBody AttestationDTO attestationDTO) throws URISyntaxException {
        LOG.debug("REST request to save Attestation : {}", attestationDTO);
        if (attestationDTO.getId() != null) {
            throw new BadRequestAlertException("A new attestation cannot already have an ID", ENTITY_NAME, "idexists");
        }
        attestationDTO = attestationService.save(attestationDTO);
        return ResponseEntity.created(new URI("/api/attestations/" + attestationDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, attestationDTO.getId().toString()))
            .body(attestationDTO);
    }

    /**
     * {@code PUT  /attestations/:id} : Updates an existing attestation.
     *
     * @param id the id of the attestationDTO to save.
     * @param attestationDTO the attestationDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated attestationDTO,
     * or with status {@code 400 (Bad Request)} if the attestationDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the attestationDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<AttestationDTO> updateAttestation(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody AttestationDTO attestationDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Attestation : {}, {}", id, attestationDTO);
        if (attestationDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, attestationDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!attestationRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        attestationDTO = attestationService.update(attestationDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, attestationDTO.getId().toString()))
            .body(attestationDTO);
    }

    /**
     * {@code PATCH  /attestations/:id} : Partial updates given fields of an existing attestation, field will ignore if it is null
     *
     * @param id the id of the attestationDTO to save.
     * @param attestationDTO the attestationDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated attestationDTO,
     * or with status {@code 400 (Bad Request)} if the attestationDTO is not valid,
     * or with status {@code 404 (Not Found)} if the attestationDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the attestationDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<AttestationDTO> partialUpdateAttestation(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody AttestationDTO attestationDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partially update Attestation : {}, {}", id, attestationDTO);
        if (attestationDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, attestationDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!attestationRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<AttestationDTO> result = attestationService.partialUpdate(attestationDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, attestationDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /attestations} : get all the Attestations.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Attestations in body.
     */
    @GetMapping("")
    public ResponseEntity<List<AttestationDTO>> getAllAttestations(@org.springdoc.core.annotations.ParameterObject Pageable pageable) {
        LOG.debug("REST request to get a page of Attestations");
        Page<AttestationDTO> page = attestationService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /attestations/:id} : get the "id" attestation.
     *
     * @param id the id of the attestationDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the attestationDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AttestationDTO> getAttestation(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get Attestation : {}", id);
        Optional<AttestationDTO> attestationDTO = attestationService.findOne(id);
        return ResponseUtil.wrapOrNotFound(attestationDTO);
    }

    /**
     * {@code DELETE  /attestations/:id} : delete the "id" attestation.
     *
     * @param id the id of the attestationDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAttestation(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete Attestation : {}", id);
        attestationService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
