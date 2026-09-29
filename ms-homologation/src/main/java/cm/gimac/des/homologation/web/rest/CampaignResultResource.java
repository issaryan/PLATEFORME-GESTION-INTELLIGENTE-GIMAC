package cm.gimac.des.homologation.web.rest;

import cm.gimac.des.homologation.repository.CampaignResultRepository;
import cm.gimac.des.homologation.service.CampaignResultService;
import cm.gimac.des.homologation.service.dto.CampaignResultDTO;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link cm.gimac.des.homologation.domain.CampaignResult}.
 */
@RestController
@RequestMapping("/api/campaign-results")
public class CampaignResultResource {

    private static final Logger LOG = LoggerFactory.getLogger(CampaignResultResource.class);

    private static final String ENTITY_NAME = "homologationCampaignResult";

    @Value("${jhipster.clientApp.name:homologation}")
    private String applicationName;

    private final CampaignResultService campaignResultService;

    private final CampaignResultRepository campaignResultRepository;

    public CampaignResultResource(CampaignResultService campaignResultService, CampaignResultRepository campaignResultRepository) {
        this.campaignResultService = campaignResultService;
        this.campaignResultRepository = campaignResultRepository;
    }

    /**
     * {@code POST  /campaign-results} : Create a new campaignResult.
     *
     * @param campaignResultDTO the campaignResultDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new campaignResultDTO, or with status {@code 400 (Bad Request)} if the campaignResult already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<CampaignResultDTO> createCampaignResult(@Valid @RequestBody CampaignResultDTO campaignResultDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save CampaignResult : {}", campaignResultDTO);
        if (campaignResultDTO.getId() != null) {
            throw new BadRequestAlertException("A new campaignResult cannot already have an ID", ENTITY_NAME, "idexists");
        }
        campaignResultDTO = campaignResultService.save(campaignResultDTO);
        return ResponseEntity.created(new URI("/api/campaign-results/" + campaignResultDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, campaignResultDTO.getId().toString()))
            .body(campaignResultDTO);
    }

    /**
     * {@code PUT  /campaign-results/:id} : Updates an existing campaignResult.
     *
     * @param id the id of the campaignResultDTO to save.
     * @param campaignResultDTO the campaignResultDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated campaignResultDTO,
     * or with status {@code 400 (Bad Request)} if the campaignResultDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the campaignResultDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<CampaignResultDTO> updateCampaignResult(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody CampaignResultDTO campaignResultDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update CampaignResult : {}, {}", id, campaignResultDTO);
        if (campaignResultDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, campaignResultDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!campaignResultRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        campaignResultDTO = campaignResultService.update(campaignResultDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, campaignResultDTO.getId().toString()))
            .body(campaignResultDTO);
    }

    /**
     * {@code PATCH  /campaign-results/:id} : Partial updates given fields of an existing campaignResult, field will ignore if it is null
     *
     * @param id the id of the campaignResultDTO to save.
     * @param campaignResultDTO the campaignResultDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated campaignResultDTO,
     * or with status {@code 400 (Bad Request)} if the campaignResultDTO is not valid,
     * or with status {@code 404 (Not Found)} if the campaignResultDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the campaignResultDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<CampaignResultDTO> partialUpdateCampaignResult(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody CampaignResultDTO campaignResultDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partially update CampaignResult : {}, {}", id, campaignResultDTO);
        if (campaignResultDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, campaignResultDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!campaignResultRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<CampaignResultDTO> result = campaignResultService.partialUpdate(campaignResultDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, campaignResultDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /campaign-results} : get all the Campaign Results.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Campaign Results in body.
     */
    @GetMapping("")
    public ResponseEntity<List<CampaignResultDTO>> getAllCampaignResults(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get a page of CampaignResults");
        Page<CampaignResultDTO> page = campaignResultService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /campaign-results/:id} : get the "id" campaignResult.
     *
     * @param id the id of the campaignResultDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the campaignResultDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CampaignResultDTO> getCampaignResult(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get CampaignResult : {}", id);
        Optional<CampaignResultDTO> campaignResultDTO = campaignResultService.findOne(id);
        return ResponseUtil.wrapOrNotFound(campaignResultDTO);
    }

    /**
     * {@code DELETE  /campaign-results/:id} : delete the "id" campaignResult.
     *
     * @param id the id of the campaignResultDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCampaignResult(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete CampaignResult : {}", id);
        campaignResultService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
