package cm.gimac.des.homologation.web.rest;

import cm.gimac.des.homologation.repository.LiaisonRepository;
import cm.gimac.des.homologation.service.LiaisonService;
import cm.gimac.des.homologation.service.dto.LiaisonDTO;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link cm.gimac.des.homologation.domain.Liaison}.
 */
@RestController
@RequestMapping("/api/liaisons")
public class LiaisonResource {

    private static final Logger LOG = LoggerFactory.getLogger(LiaisonResource.class);

    private static final String ENTITY_NAME = "homologationLiaison";

    @Value("${jhipster.clientApp.name:homologation}")
    private String applicationName;

    private final LiaisonService liaisonService;

    private final LiaisonRepository liaisonRepository;

    public LiaisonResource(LiaisonService liaisonService, LiaisonRepository liaisonRepository) {
        this.liaisonService = liaisonService;
        this.liaisonRepository = liaisonRepository;
    }

    /**
     * {@code POST  /liaisons} : Create a new liaison.
     *
     * @param liaisonDTO the liaisonDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new liaisonDTO, or with status {@code 400 (Bad Request)} if the liaison already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<LiaisonDTO> createLiaison(@Valid @RequestBody LiaisonDTO liaisonDTO) throws URISyntaxException {
        LOG.debug("REST request to save Liaison : {}", liaisonDTO);
        if (liaisonDTO.getId() != null) {
            throw new BadRequestAlertException("A new liaison cannot already have an ID", ENTITY_NAME, "idexists");
        }
        liaisonDTO = liaisonService.save(liaisonDTO);
        return ResponseEntity.created(new URI("/api/liaisons/" + liaisonDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, liaisonDTO.getId().toString()))
            .body(liaisonDTO);
    }

    /**
     * {@code PUT  /liaisons/:id} : Updates an existing liaison.
     *
     * @param id the id of the liaisonDTO to save.
     * @param liaisonDTO the liaisonDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated liaisonDTO,
     * or with status {@code 400 (Bad Request)} if the liaisonDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the liaisonDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<LiaisonDTO> updateLiaison(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody LiaisonDTO liaisonDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Liaison : {}, {}", id, liaisonDTO);
        if (liaisonDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, liaisonDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!liaisonRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        liaisonDTO = liaisonService.update(liaisonDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, liaisonDTO.getId().toString()))
            .body(liaisonDTO);
    }

    /**
     * {@code PATCH  /liaisons/:id} : Partial updates given fields of an existing liaison, field will ignore if it is null
     *
     * @param id the id of the liaisonDTO to save.
     * @param liaisonDTO the liaisonDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated liaisonDTO,
     * or with status {@code 400 (Bad Request)} if the liaisonDTO is not valid,
     * or with status {@code 404 (Not Found)} if the liaisonDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the liaisonDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<LiaisonDTO> partialUpdateLiaison(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody LiaisonDTO liaisonDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partially update Liaison : {}, {}", id, liaisonDTO);
        if (liaisonDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, liaisonDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!liaisonRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<LiaisonDTO> result = liaisonService.partialUpdate(liaisonDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, liaisonDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /liaisons} : get all the Liaisons.
     *
     * @param pageable the pagination information.
     * @param filter the filter of the request.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Liaisons in body.
     */
    @GetMapping("")
    public ResponseEntity<List<LiaisonDTO>> getAllLiaisons(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        @RequestParam(name = "filter", required = false) String filter
    ) {
        if ("contractualscope-is-null".equals(filter)) {
            LOG.debug("REST request to get all Liaisons where contractualScope is null");
            return new ResponseEntity<>(liaisonService.findAllWhereContractualScopeIsNull(), HttpStatus.OK);
        }
        LOG.debug("REST request to get a page of Liaisons");
        Page<LiaisonDTO> page = liaisonService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /liaisons/:id} : get the "id" liaison.
     *
     * @param id the id of the liaisonDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the liaisonDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<LiaisonDTO> getLiaison(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get Liaison : {}", id);
        Optional<LiaisonDTO> liaisonDTO = liaisonService.findOne(id);
        return ResponseUtil.wrapOrNotFound(liaisonDTO);
    }

    /**
     * {@code DELETE  /liaisons/:id} : delete the "id" liaison.
     *
     * @param id the id of the liaisonDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLiaison(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete Liaison : {}", id);
        liaisonService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
