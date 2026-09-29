package cm.gimac.des.certification.web.rest;

import cm.gimac.des.certification.repository.CertificationRegisterEntryRepository;
import cm.gimac.des.certification.service.CertificationRegisterEntryService;
import cm.gimac.des.certification.service.dto.CertificationRegisterEntryDTO;
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
 * REST controller for managing {@link cm.gimac.des.certification.domain.CertificationRegisterEntry}.
 */
@RestController
@RequestMapping("/api/certification-register-entries")
public class CertificationRegisterEntryResource {

    private static final Logger LOG = LoggerFactory.getLogger(CertificationRegisterEntryResource.class);

    private static final String ENTITY_NAME = "certificationCertificationRegisterEntry";

    @Value("${jhipster.clientApp.name:certification}")
    private String applicationName;

    private final CertificationRegisterEntryService certificationRegisterEntryService;

    private final CertificationRegisterEntryRepository certificationRegisterEntryRepository;

    public CertificationRegisterEntryResource(
        CertificationRegisterEntryService certificationRegisterEntryService,
        CertificationRegisterEntryRepository certificationRegisterEntryRepository
    ) {
        this.certificationRegisterEntryService = certificationRegisterEntryService;
        this.certificationRegisterEntryRepository = certificationRegisterEntryRepository;
    }

    /**
     * {@code POST  /certification-register-entries} : Create a new certificationRegisterEntry.
     *
     * @param certificationRegisterEntryDTO the certificationRegisterEntryDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new certificationRegisterEntryDTO, or with status {@code 400 (Bad Request)} if the certificationRegisterEntry already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<CertificationRegisterEntryDTO> createCertificationRegisterEntry(
        @Valid @RequestBody CertificationRegisterEntryDTO certificationRegisterEntryDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to save CertificationRegisterEntry : {}", certificationRegisterEntryDTO);
        if (certificationRegisterEntryDTO.getId() != null) {
            throw new BadRequestAlertException("A new certificationRegisterEntry cannot already have an ID", ENTITY_NAME, "idexists");
        }
        certificationRegisterEntryDTO = certificationRegisterEntryService.save(certificationRegisterEntryDTO);
        return ResponseEntity.created(new URI("/api/certification-register-entries/" + certificationRegisterEntryDTO.getId()))
            .headers(
                HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, certificationRegisterEntryDTO.getId().toString())
            )
            .body(certificationRegisterEntryDTO);
    }

    /**
     * {@code PUT  /certification-register-entries/:id} : Updates an existing certificationRegisterEntry.
     *
     * @param id the id of the certificationRegisterEntryDTO to save.
     * @param certificationRegisterEntryDTO the certificationRegisterEntryDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated certificationRegisterEntryDTO,
     * or with status {@code 400 (Bad Request)} if the certificationRegisterEntryDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the certificationRegisterEntryDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<CertificationRegisterEntryDTO> updateCertificationRegisterEntry(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody CertificationRegisterEntryDTO certificationRegisterEntryDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update CertificationRegisterEntry : {}, {}", id, certificationRegisterEntryDTO);
        if (certificationRegisterEntryDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, certificationRegisterEntryDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!certificationRegisterEntryRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        certificationRegisterEntryDTO = certificationRegisterEntryService.update(certificationRegisterEntryDTO);
        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, certificationRegisterEntryDTO.getId().toString())
            )
            .body(certificationRegisterEntryDTO);
    }

    /**
     * {@code PATCH  /certification-register-entries/:id} : Partial updates given fields of an existing certificationRegisterEntry, field will ignore if it is null
     *
     * @param id the id of the certificationRegisterEntryDTO to save.
     * @param certificationRegisterEntryDTO the certificationRegisterEntryDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated certificationRegisterEntryDTO,
     * or with status {@code 400 (Bad Request)} if the certificationRegisterEntryDTO is not valid,
     * or with status {@code 404 (Not Found)} if the certificationRegisterEntryDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the certificationRegisterEntryDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<CertificationRegisterEntryDTO> partialUpdateCertificationRegisterEntry(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody CertificationRegisterEntryDTO certificationRegisterEntryDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partially update CertificationRegisterEntry : {}, {}", id, certificationRegisterEntryDTO);
        if (certificationRegisterEntryDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, certificationRegisterEntryDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!certificationRegisterEntryRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<CertificationRegisterEntryDTO> result = certificationRegisterEntryService.partialUpdate(certificationRegisterEntryDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, certificationRegisterEntryDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /certification-register-entries} : get all the Certification Register Entries.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Certification Register Entries in body.
     */
    @GetMapping("")
    public ResponseEntity<List<CertificationRegisterEntryDTO>> getAllCertificationRegisterEntries(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get a page of CertificationRegisterEntries");
        Page<CertificationRegisterEntryDTO> page = certificationRegisterEntryService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /certification-register-entries/:id} : get the "id" certificationRegisterEntry.
     *
     * @param id the id of the certificationRegisterEntryDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the certificationRegisterEntryDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CertificationRegisterEntryDTO> getCertificationRegisterEntry(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get CertificationRegisterEntry : {}", id);
        Optional<CertificationRegisterEntryDTO> certificationRegisterEntryDTO = certificationRegisterEntryService.findOne(id);
        return ResponseUtil.wrapOrNotFound(certificationRegisterEntryDTO);
    }

    /**
     * {@code DELETE  /certification-register-entries/:id} : delete the "id" certificationRegisterEntry.
     *
     * @param id the id of the certificationRegisterEntryDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCertificationRegisterEntry(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete CertificationRegisterEntry : {}", id);
        certificationRegisterEntryService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
