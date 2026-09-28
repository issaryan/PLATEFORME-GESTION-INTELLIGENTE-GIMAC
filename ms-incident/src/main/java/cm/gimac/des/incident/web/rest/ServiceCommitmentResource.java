package cm.gimac.des.incident.web.rest;

import cm.gimac.des.incident.repository.ServiceCommitmentRepository;
import cm.gimac.des.incident.service.ServiceCommitmentService;
import cm.gimac.des.incident.service.dto.ServiceCommitmentDTO;
import cm.gimac.des.incident.web.rest.errors.BadRequestAlertException;
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
 * REST controller for managing {@link cm.gimac.des.incident.domain.ServiceCommitment}.
 */
@RestController
@RequestMapping("/api/service-commitments")
public class ServiceCommitmentResource {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceCommitmentResource.class);

    private static final String ENTITY_NAME = "incidentServiceCommitment";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ServiceCommitmentService serviceCommitmentService;

    private final ServiceCommitmentRepository serviceCommitmentRepository;

    public ServiceCommitmentResource(
        ServiceCommitmentService serviceCommitmentService,
        ServiceCommitmentRepository serviceCommitmentRepository
    ) {
        this.serviceCommitmentService = serviceCommitmentService;
        this.serviceCommitmentRepository = serviceCommitmentRepository;
    }

    /**
     * {@code POST  /service-commitments} : Create a new serviceCommitment.
     *
     * @param serviceCommitmentDTO the serviceCommitmentDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new serviceCommitmentDTO, or with status {@code 400 (Bad Request)} if the serviceCommitment has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<ServiceCommitmentDTO> createServiceCommitment(@Valid @RequestBody ServiceCommitmentDTO serviceCommitmentDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save ServiceCommitment : {}", serviceCommitmentDTO);
        if (serviceCommitmentDTO.getId() != null) {
            throw new BadRequestAlertException("A new serviceCommitment cannot already have an ID", ENTITY_NAME, "idexists");
        }
        serviceCommitmentDTO = serviceCommitmentService.save(serviceCommitmentDTO);
        return ResponseEntity.created(new URI("/api/service-commitments/" + serviceCommitmentDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, serviceCommitmentDTO.getId().toString()))
            .body(serviceCommitmentDTO);
    }

    /**
     * {@code PUT  /service-commitments/:id} : Updates an existing serviceCommitment.
     *
     * @param id the id of the serviceCommitmentDTO to save.
     * @param serviceCommitmentDTO the serviceCommitmentDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated serviceCommitmentDTO,
     * or with status {@code 400 (Bad Request)} if the serviceCommitmentDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the serviceCommitmentDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ServiceCommitmentDTO> updateServiceCommitment(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody ServiceCommitmentDTO serviceCommitmentDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update ServiceCommitment : {}, {}", id, serviceCommitmentDTO);
        if (serviceCommitmentDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, serviceCommitmentDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!serviceCommitmentRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        serviceCommitmentDTO = serviceCommitmentService.update(serviceCommitmentDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, serviceCommitmentDTO.getId().toString()))
            .body(serviceCommitmentDTO);
    }

    /**
     * {@code PATCH  /service-commitments/:id} : Partial updates given fields of an existing serviceCommitment, field will ignore if it is null
     *
     * @param id the id of the serviceCommitmentDTO to save.
     * @param serviceCommitmentDTO the serviceCommitmentDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated serviceCommitmentDTO,
     * or with status {@code 400 (Bad Request)} if the serviceCommitmentDTO is not valid,
     * or with status {@code 404 (Not Found)} if the serviceCommitmentDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the serviceCommitmentDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<ServiceCommitmentDTO> partialUpdateServiceCommitment(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody ServiceCommitmentDTO serviceCommitmentDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update ServiceCommitment partially : {}, {}", id, serviceCommitmentDTO);
        if (serviceCommitmentDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, serviceCommitmentDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!serviceCommitmentRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<ServiceCommitmentDTO> result = serviceCommitmentService.partialUpdate(serviceCommitmentDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, serviceCommitmentDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /service-commitments} : get all the serviceCommitments.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of serviceCommitments in body.
     */
    @GetMapping("")
    public List<ServiceCommitmentDTO> getAllServiceCommitments() {
        LOG.debug("REST request to get all ServiceCommitments");
        return serviceCommitmentService.findAll();
    }

    /**
     * {@code GET  /service-commitments/:id} : get the "id" serviceCommitment.
     *
     * @param id the id of the serviceCommitmentDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the serviceCommitmentDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ServiceCommitmentDTO> getServiceCommitment(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get ServiceCommitment : {}", id);
        Optional<ServiceCommitmentDTO> serviceCommitmentDTO = serviceCommitmentService.findOne(id);
        return ResponseUtil.wrapOrNotFound(serviceCommitmentDTO);
    }

    /**
     * {@code DELETE  /service-commitments/:id} : delete the "id" serviceCommitment.
     *
     * @param id the id of the serviceCommitmentDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteServiceCommitment(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete ServiceCommitment : {}", id);
        serviceCommitmentService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
