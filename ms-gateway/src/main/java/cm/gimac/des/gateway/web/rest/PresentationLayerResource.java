package cm.gimac.des.gateway.web.rest;

import cm.gimac.des.gateway.repository.PresentationLayerRepository;
import cm.gimac.des.gateway.service.PresentationLayerService;
import cm.gimac.des.gateway.service.dto.PresentationLayerDTO;
import cm.gimac.des.gateway.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.reactive.ResponseUtil;

/**
 * REST controller for managing {@link cm.gimac.des.gateway.domain.PresentationLayer}.
 */
@RestController
@RequestMapping("/api/presentation-layers")
public class PresentationLayerResource {

    private static final Logger LOG = LoggerFactory.getLogger(PresentationLayerResource.class);

    private static final String ENTITY_NAME = "presentationLayer";

    @Value("${jhipster.clientApp.name:gateway}")
    private String applicationName;

    private final PresentationLayerService presentationLayerService;

    private final PresentationLayerRepository presentationLayerRepository;

    public PresentationLayerResource(
        PresentationLayerService presentationLayerService,
        PresentationLayerRepository presentationLayerRepository
    ) {
        this.presentationLayerService = presentationLayerService;
        this.presentationLayerRepository = presentationLayerRepository;
    }

    /**
     * {@code POST  /presentation-layers} : Create a new presentationLayer.
     *
     * @param presentationLayerDTO the presentationLayerDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new presentationLayerDTO, or with status {@code 400 (Bad Request)} if the presentationLayer already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public Mono<ResponseEntity<PresentationLayerDTO>> createPresentationLayer(@Valid @RequestBody PresentationLayerDTO presentationLayerDTO)
        throws URISyntaxException {
        LOG.debug("REST request to save PresentationLayer : {}", presentationLayerDTO);
        if (presentationLayerDTO.getId() != null) {
            throw new BadRequestAlertException("A new presentationLayer cannot already have an ID", ENTITY_NAME, "idexists");
        }
        return presentationLayerService.save(presentationLayerDTO).map(result -> {
            try {
                return ResponseEntity.created(new URI("/api/presentation-layers/" + result.getId()))
                    .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
                    .body(result);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * {@code PUT  /presentation-layers/:id} : Updates an existing presentationLayer.
     *
     * @param id the id of the presentationLayerDTO to save.
     * @param presentationLayerDTO the presentationLayerDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated presentationLayerDTO,
     * or with status {@code 400 (Bad Request)} if the presentationLayerDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the presentationLayerDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public Mono<ResponseEntity<PresentationLayerDTO>> updatePresentationLayer(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody PresentationLayerDTO presentationLayerDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update PresentationLayer : {}, {}", id, presentationLayerDTO);
        if (presentationLayerDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, presentationLayerDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        return presentationLayerRepository.existsById(id).flatMap(exists -> {
            if (!exists) {
                return Mono.error(new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));
            }

            return presentationLayerService
                .update(presentationLayerDTO)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                .map(result ->
                    ResponseEntity.ok()
                        .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
                        .body(result)
                );
        });
    }

    /**
     * {@code PATCH  /presentation-layers/:id} : Partial updates given fields of an existing presentationLayer, field will ignore if it is null
     *
     * @param id the id of the presentationLayerDTO to save.
     * @param presentationLayerDTO the presentationLayerDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated presentationLayerDTO,
     * or with status {@code 400 (Bad Request)} if the presentationLayerDTO is not valid,
     * or with status {@code 404 (Not Found)} if the presentationLayerDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the presentationLayerDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public Mono<ResponseEntity<PresentationLayerDTO>> partialUpdatePresentationLayer(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody PresentationLayerDTO presentationLayerDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partially update PresentationLayer : {}, {}", id, presentationLayerDTO);
        if (presentationLayerDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, presentationLayerDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        return presentationLayerRepository.existsById(id).flatMap(exists -> {
            if (!exists) {
                return Mono.error(new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));
            }

            Mono<PresentationLayerDTO> result = presentationLayerService.partialUpdate(presentationLayerDTO);

            return result.switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND))).map(res ->
                ResponseEntity.ok()
                    .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, res.getId().toString()))
                    .body(res)
            );
        });
    }

    /**
     * {@code GET  /presentation-layers} : get all the Presentation Layers.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Presentation Layers in body.
     */
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<List<PresentationLayerDTO>> getAllPresentationLayers() {
        LOG.debug("REST request to get all PresentationLayers");
        return presentationLayerService.findAll().collectList();
    }

    /**
     * {@code GET  /presentation-layers} : get all the Presentation Layers as a stream.
     * @return the {@link Flux} of Presentation Layers.
     */
    @GetMapping(value = "", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<PresentationLayerDTO> getAllPresentationLayersAsStream() {
        LOG.debug("REST request to get all PresentationLayers as a stream");
        return presentationLayerService.findAll();
    }

    /**
     * {@code GET  /presentation-layers/:id} : get the "id" presentationLayer.
     *
     * @param id the id of the presentationLayerDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the presentationLayerDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<PresentationLayerDTO>> getPresentationLayer(@PathVariable("id") Long id) {
        LOG.debug("REST request to get PresentationLayer : {}", id);
        Mono<PresentationLayerDTO> presentationLayerDTO = presentationLayerService.findOne(id);
        return ResponseUtil.wrapOrNotFound(presentationLayerDTO);
    }

    /**
     * {@code DELETE  /presentation-layers/:id} : delete the "id" presentationLayer.
     *
     * @param id the id of the presentationLayerDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deletePresentationLayer(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete PresentationLayer : {}", id);
        return presentationLayerService
            .delete(id)

            .then(
                Mono.just(
                    ResponseEntity.noContent()
                        .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
                        .build()
                )
            );
    }
}
