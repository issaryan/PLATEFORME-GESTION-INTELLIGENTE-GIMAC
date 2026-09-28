package cm.gimac.des.iam.web.rest;

import cm.gimac.des.iam.repository.AccessReviewRepository;
import cm.gimac.des.iam.service.AccessReviewService;
import cm.gimac.des.iam.service.dto.AccessReviewDTO;
import cm.gimac.des.iam.web.rest.errors.BadRequestAlertException;
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
 * REST controller for managing {@link cm.gimac.des.iam.domain.AccessReview}.
 */
@RestController
@RequestMapping("/api/access-reviews")
public class AccessReviewResource {

    private static final Logger LOG = LoggerFactory.getLogger(AccessReviewResource.class);

    private static final String ENTITY_NAME = "iamAccessReview";

    @Value("${jhipster.clientApp.name:iam}")
    private String applicationName;

    private final AccessReviewService accessReviewService;

    private final AccessReviewRepository accessReviewRepository;

    public AccessReviewResource(AccessReviewService accessReviewService, AccessReviewRepository accessReviewRepository) {
        this.accessReviewService = accessReviewService;
        this.accessReviewRepository = accessReviewRepository;
    }

    /**
     * {@code POST  /access-reviews} : Create a new accessReview.
     *
     * @param accessReviewDTO the accessReviewDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new accessReviewDTO, or with status {@code 400 (Bad Request)} if the accessReview already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<AccessReviewDTO> createAccessReview(@Valid @RequestBody AccessReviewDTO accessReviewDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save AccessReview : {}", accessReviewDTO);
        if (accessReviewDTO.getId() != null) {
            throw new BadRequestAlertException("A new accessReview cannot already have an ID", ENTITY_NAME, "idexists");
        }
        accessReviewDTO = accessReviewService.save(accessReviewDTO);
        return ResponseEntity.created(new URI("/api/access-reviews/" + accessReviewDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, accessReviewDTO.getId().toString()))
            .body(accessReviewDTO);
    }

    /**
     * {@code PUT  /access-reviews/:id} : Updates an existing accessReview.
     *
     * @param id the id of the accessReviewDTO to save.
     * @param accessReviewDTO the accessReviewDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated accessReviewDTO,
     * or with status {@code 400 (Bad Request)} if the accessReviewDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the accessReviewDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<AccessReviewDTO> updateAccessReview(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody AccessReviewDTO accessReviewDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update AccessReview : {}, {}", id, accessReviewDTO);
        if (accessReviewDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, accessReviewDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!accessReviewRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        accessReviewDTO = accessReviewService.update(accessReviewDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, accessReviewDTO.getId().toString()))
            .body(accessReviewDTO);
    }

    /**
     * {@code PATCH  /access-reviews/:id} : Partial updates given fields of an existing accessReview, field will ignore if it is null
     *
     * @param id the id of the accessReviewDTO to save.
     * @param accessReviewDTO the accessReviewDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated accessReviewDTO,
     * or with status {@code 400 (Bad Request)} if the accessReviewDTO is not valid,
     * or with status {@code 404 (Not Found)} if the accessReviewDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the accessReviewDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<AccessReviewDTO> partialUpdateAccessReview(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody AccessReviewDTO accessReviewDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partially update AccessReview : {}, {}", id, accessReviewDTO);
        if (accessReviewDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, accessReviewDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!accessReviewRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<AccessReviewDTO> result = accessReviewService.partialUpdate(accessReviewDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, accessReviewDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /access-reviews} : get all the Access Reviews.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Access Reviews in body.
     */
    @GetMapping("")
    public ResponseEntity<List<AccessReviewDTO>> getAllAccessReviews(@org.springdoc.core.annotations.ParameterObject Pageable pageable) {
        LOG.debug("REST request to get a page of AccessReviews");
        Page<AccessReviewDTO> page = accessReviewService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /access-reviews/:id} : get the "id" accessReview.
     *
     * @param id the id of the accessReviewDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the accessReviewDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AccessReviewDTO> getAccessReview(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get AccessReview : {}", id);
        Optional<AccessReviewDTO> accessReviewDTO = accessReviewService.findOne(id);
        return ResponseUtil.wrapOrNotFound(accessReviewDTO);
    }

    /**
     * {@code DELETE  /access-reviews/:id} : delete the "id" accessReview.
     *
     * @param id the id of the accessReviewDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccessReview(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete AccessReview : {}", id);
        accessReviewService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
