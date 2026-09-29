package cm.gimac.des.participant.web.rest;

import cm.gimac.des.participant.repository.CorrespondentRepository;
import cm.gimac.des.participant.service.CorrespondentQueryService;
import cm.gimac.des.participant.service.CorrespondentService;
import cm.gimac.des.participant.service.criteria.CorrespondentCriteria;
import cm.gimac.des.participant.service.dto.CorrespondentDTO;
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
 * REST controller for managing {@link cm.gimac.des.participant.domain.Correspondent}.
 */
@RestController
@RequestMapping("/api/correspondents")
public class CorrespondentResource {

    private static final Logger LOG = LoggerFactory.getLogger(CorrespondentResource.class);

    private static final String ENTITY_NAME = "participantCorrespondent";

    @Value("${jhipster.clientApp.name:participant}")
    private String applicationName;

    private final CorrespondentService correspondentService;

    private final CorrespondentRepository correspondentRepository;

    private final CorrespondentQueryService correspondentQueryService;

    public CorrespondentResource(
        CorrespondentService correspondentService,
        CorrespondentRepository correspondentRepository,
        CorrespondentQueryService correspondentQueryService
    ) {
        this.correspondentService = correspondentService;
        this.correspondentRepository = correspondentRepository;
        this.correspondentQueryService = correspondentQueryService;
    }

    /**
     * {@code POST  /correspondents} : Create a new correspondent.
     *
     * @param correspondentDTO the correspondentDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new correspondentDTO, or with status {@code 400 (Bad Request)} if the correspondent already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<CorrespondentDTO> createCorrespondent(@Valid @RequestBody CorrespondentDTO correspondentDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save Correspondent : {}", correspondentDTO);
        if (correspondentDTO.getId() != null) {
            throw new BadRequestAlertException("A new correspondent cannot already have an ID", ENTITY_NAME, "idexists");
        }
        correspondentDTO = correspondentService.save(correspondentDTO);
        return ResponseEntity.created(new URI("/api/correspondents/" + correspondentDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, correspondentDTO.getId().toString()))
            .body(correspondentDTO);
    }

    /**
     * {@code PUT  /correspondents/:id} : Updates an existing correspondent.
     *
     * @param id the id of the correspondentDTO to save.
     * @param correspondentDTO the correspondentDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated correspondentDTO,
     * or with status {@code 400 (Bad Request)} if the correspondentDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the correspondentDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<CorrespondentDTO> updateCorrespondent(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody CorrespondentDTO correspondentDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Correspondent : {}, {}", id, correspondentDTO);
        if (correspondentDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, correspondentDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!correspondentRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        correspondentDTO = correspondentService.update(correspondentDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, correspondentDTO.getId().toString()))
            .body(correspondentDTO);
    }

    /**
     * {@code PATCH  /correspondents/:id} : Partial updates given fields of an existing correspondent, field will ignore if it is null
     *
     * @param id the id of the correspondentDTO to save.
     * @param correspondentDTO the correspondentDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated correspondentDTO,
     * or with status {@code 400 (Bad Request)} if the correspondentDTO is not valid,
     * or with status {@code 404 (Not Found)} if the correspondentDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the correspondentDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<CorrespondentDTO> partialUpdateCorrespondent(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody CorrespondentDTO correspondentDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partially update Correspondent : {}, {}", id, correspondentDTO);
        if (correspondentDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, correspondentDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!correspondentRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<CorrespondentDTO> result = correspondentService.partialUpdate(correspondentDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, correspondentDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /correspondents} : get all the Correspondents.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Correspondents in body.
     */
    @GetMapping("")
    public ResponseEntity<List<CorrespondentDTO>> getAllCorrespondents(
        CorrespondentCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get Correspondents by criteria: {}", criteria);

        Page<CorrespondentDTO> page = correspondentQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /correspondents/count} : count all the correspondents.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countCorrespondents(CorrespondentCriteria criteria) {
        LOG.debug("REST request to count Correspondents by criteria: {}", criteria);
        return ResponseEntity.ok().body(correspondentQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /correspondents/:id} : get the "id" correspondent.
     *
     * @param id the id of the correspondentDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the correspondentDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CorrespondentDTO> getCorrespondent(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get Correspondent : {}", id);
        Optional<CorrespondentDTO> correspondentDTO = correspondentService.findOne(id);
        return ResponseUtil.wrapOrNotFound(correspondentDTO);
    }

    /**
     * {@code DELETE  /correspondents/:id} : delete the "id" correspondent.
     *
     * @param id the id of the correspondentDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCorrespondent(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete Correspondent : {}", id);
        correspondentService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
