package cm.gimac.des.certification.web.rest;

import cm.gimac.des.certification.repository.TimestampTokenRepository;
import cm.gimac.des.certification.service.TimestampTokenService;
import cm.gimac.des.certification.service.dto.TimestampTokenDTO;
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
 * REST controller for managing {@link cm.gimac.des.certification.domain.TimestampToken}.
 */
@RestController
@RequestMapping("/api/timestamp-tokens")
public class TimestampTokenResource {

    private static final Logger LOG = LoggerFactory.getLogger(TimestampTokenResource.class);

    private static final String ENTITY_NAME = "certificationTimestampToken";

    @Value("${jhipster.clientApp.name:certification}")
    private String applicationName;

    private final TimestampTokenService timestampTokenService;

    private final TimestampTokenRepository timestampTokenRepository;

    public TimestampTokenResource(TimestampTokenService timestampTokenService, TimestampTokenRepository timestampTokenRepository) {
        this.timestampTokenService = timestampTokenService;
        this.timestampTokenRepository = timestampTokenRepository;
    }

    /**
     * {@code POST  /timestamp-tokens} : Create a new timestampToken.
     *
     * @param timestampTokenDTO the timestampTokenDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new timestampTokenDTO, or with status {@code 400 (Bad Request)} if the timestampToken already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<TimestampTokenDTO> createTimestampToken(@Valid @RequestBody TimestampTokenDTO timestampTokenDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save TimestampToken : {}", timestampTokenDTO);
        if (timestampTokenDTO.getId() != null) {
            throw new BadRequestAlertException("A new timestampToken cannot already have an ID", ENTITY_NAME, "idexists");
        }
        timestampTokenDTO = timestampTokenService.save(timestampTokenDTO);
        return ResponseEntity.created(new URI("/api/timestamp-tokens/" + timestampTokenDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, timestampTokenDTO.getId().toString()))
            .body(timestampTokenDTO);
    }

    /**
     * {@code PUT  /timestamp-tokens/:id} : Updates an existing timestampToken.
     *
     * @param id the id of the timestampTokenDTO to save.
     * @param timestampTokenDTO the timestampTokenDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated timestampTokenDTO,
     * or with status {@code 400 (Bad Request)} if the timestampTokenDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the timestampTokenDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<TimestampTokenDTO> updateTimestampToken(
        @PathVariable(value = "id", required = false) final UUID id,
        @Valid @RequestBody TimestampTokenDTO timestampTokenDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update TimestampToken : {}, {}", id, timestampTokenDTO);
        if (timestampTokenDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, timestampTokenDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!timestampTokenRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        timestampTokenDTO = timestampTokenService.update(timestampTokenDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, timestampTokenDTO.getId().toString()))
            .body(timestampTokenDTO);
    }

    /**
     * {@code PATCH  /timestamp-tokens/:id} : Partial updates given fields of an existing timestampToken, field will ignore if it is null
     *
     * @param id the id of the timestampTokenDTO to save.
     * @param timestampTokenDTO the timestampTokenDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated timestampTokenDTO,
     * or with status {@code 400 (Bad Request)} if the timestampTokenDTO is not valid,
     * or with status {@code 404 (Not Found)} if the timestampTokenDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the timestampTokenDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<TimestampTokenDTO> partialUpdateTimestampToken(
        @PathVariable(value = "id", required = false) final UUID id,
        @NotNull @RequestBody TimestampTokenDTO timestampTokenDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partially update TimestampToken : {}, {}", id, timestampTokenDTO);
        if (timestampTokenDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, timestampTokenDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!timestampTokenRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<TimestampTokenDTO> result = timestampTokenService.partialUpdate(timestampTokenDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, timestampTokenDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /timestamp-tokens} : get all the Timestamp Tokens.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Timestamp Tokens in body.
     */
    @GetMapping("")
    public List<TimestampTokenDTO> getAllTimestampTokens() {
        LOG.debug("REST request to get all TimestampTokens");
        return timestampTokenService.findAll();
    }

    /**
     * {@code GET  /timestamp-tokens/:id} : get the "id" timestampToken.
     *
     * @param id the id of the timestampTokenDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the timestampTokenDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<TimestampTokenDTO> getTimestampToken(@PathVariable("id") UUID id) {
        LOG.debug("REST request to get TimestampToken : {}", id);
        Optional<TimestampTokenDTO> timestampTokenDTO = timestampTokenService.findOne(id);
        return ResponseUtil.wrapOrNotFound(timestampTokenDTO);
    }

    /**
     * {@code DELETE  /timestamp-tokens/:id} : delete the "id" timestampToken.
     *
     * @param id the id of the timestampTokenDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTimestampToken(@PathVariable("id") UUID id) {
        LOG.debug("REST request to delete TimestampToken : {}", id);
        timestampTokenService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
