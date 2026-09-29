package cm.gimac.des.certification.web.rest;

import cm.gimac.des.certification.repository.CertificationCircuitRepository;
import cm.gimac.des.certification.service.CertificationCircuitService;
import cm.gimac.des.certification.service.dto.CertificationCircuitDTO;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link cm.gimac.des.certification.domain.CertificationCircuit}.
 */
@RestController
@RequestMapping("/api/certification-circuits")
public class CertificationCircuitResource {

    private static final Logger LOG = LoggerFactory.getLogger(CertificationCircuitResource.class);

    private static final String ENTITY_NAME = "certificationCertificationCircuit";

    @Value("${jhipster.clientApp.name:certification}")
    private String applicationName;

    private final CertificationCircuitService certificationCircuitService;

    private final CertificationCircuitRepository certificationCircuitRepository;

    public CertificationCircuitResource(
        CertificationCircuitService certificationCircuitService,
        CertificationCircuitRepository certificationCircuitRepository
    ) {
        this.certificationCircuitService = certificationCircuitService;
        this.certificationCircuitRepository = certificationCircuitRepository;
    }

    /**
     * {@code POST  /certification-circuits} : Create a new certificationCircuit.
     *
     * @param certificationCircuitDTO the certificationCircuitDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new certificationCircuitDTO, or with status {@code 400 (Bad Request)} if the certificationCircuit already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<CertificationCircuitDTO> createCertificationCircuit(
        @Valid @RequestBody CertificationCircuitDTO certificationCircuitDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to save CertificationCircuit : {}", certificationCircuitDTO);
        if (certificationCircuitDTO.getId() != null) {
            throw new BadRequestAlertException("A new certificationCircuit cannot already have an ID", ENTITY_NAME, "idexists");
        }
        certificationCircuitDTO = certificationCircuitService.save(certificationCircuitDTO);
        return ResponseEntity.created(new URI("/api/certification-circuits/" + certificationCircuitDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, certificationCircuitDTO.getId().toString()))
            .body(certificationCircuitDTO);
    }

    /**
     * {@code PUT  /certification-circuits/:id} : Updates an existing certificationCircuit.
     *
     * @param id the id of the certificationCircuitDTO to save.
     * @param certificationCircuitDTO the certificationCircuitDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated certificationCircuitDTO,
     * or with status {@code 400 (Bad Request)} if the certificationCircuitDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the certificationCircuitDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<CertificationCircuitDTO> updateCertificationCircuit(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody CertificationCircuitDTO certificationCircuitDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update CertificationCircuit : {}, {}", id, certificationCircuitDTO);
        if (certificationCircuitDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, certificationCircuitDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!certificationCircuitRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        certificationCircuitDTO = certificationCircuitService.update(certificationCircuitDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, certificationCircuitDTO.getId().toString()))
            .body(certificationCircuitDTO);
    }

    /**
     * {@code PATCH  /certification-circuits/:id} : Partial updates given fields of an existing certificationCircuit, field will ignore if it is null
     *
     * @param id the id of the certificationCircuitDTO to save.
     * @param certificationCircuitDTO the certificationCircuitDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated certificationCircuitDTO,
     * or with status {@code 400 (Bad Request)} if the certificationCircuitDTO is not valid,
     * or with status {@code 404 (Not Found)} if the certificationCircuitDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the certificationCircuitDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<CertificationCircuitDTO> partialUpdateCertificationCircuit(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody CertificationCircuitDTO certificationCircuitDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partially update CertificationCircuit : {}, {}", id, certificationCircuitDTO);
        if (certificationCircuitDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, certificationCircuitDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!certificationCircuitRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<CertificationCircuitDTO> result = certificationCircuitService.partialUpdate(certificationCircuitDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, certificationCircuitDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /certification-circuits} : get all the Certification Circuits.
     *
     * @param filter the filter of the request.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Certification Circuits in body.
     */
    @GetMapping("")
    public List<CertificationCircuitDTO> getAllCertificationCircuits(@RequestParam(name = "filter", required = false) String filter) {
        if ("golivedossier-is-null".equals(filter)) {
            LOG.debug("REST request to get all CertificationCircuits where goLiveDossier is null");
            return certificationCircuitService.findAllWhereGoLiveDossierIsNull();
        }
        LOG.debug("REST request to get all CertificationCircuits");
        return certificationCircuitService.findAll();
    }

    /**
     * {@code GET  /certification-circuits/:id} : get the "id" certificationCircuit.
     *
     * @param id the id of the certificationCircuitDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the certificationCircuitDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CertificationCircuitDTO> getCertificationCircuit(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get CertificationCircuit : {}", id);
        Optional<CertificationCircuitDTO> certificationCircuitDTO = certificationCircuitService.findOne(id);
        return ResponseUtil.wrapOrNotFound(certificationCircuitDTO);
    }

    /**
     * {@code DELETE  /certification-circuits/:id} : delete the "id" certificationCircuit.
     *
     * @param id the id of the certificationCircuitDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCertificationCircuit(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete CertificationCircuit : {}", id);
        certificationCircuitService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
