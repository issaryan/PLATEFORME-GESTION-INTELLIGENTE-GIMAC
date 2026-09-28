package cm.gimac.des.gateway.web.rest;

import cm.gimac.des.gateway.repository.RouteRepository;
import cm.gimac.des.gateway.service.RouteService;
import cm.gimac.des.gateway.service.dto.RouteDTO;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.ForwardedHeaderUtils;
import reactor.core.publisher.Mono;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.reactive.ResponseUtil;

/**
 * REST controller for managing {@link cm.gimac.des.gateway.domain.Route}.
 */
@RestController
@RequestMapping("/api/routes")
public class RouteResource {

    private static final Logger LOG = LoggerFactory.getLogger(RouteResource.class);

    private static final String ENTITY_NAME = "route";

    @Value("${jhipster.clientApp.name:gateway}")
    private String applicationName;

    private final RouteService routeService;

    private final RouteRepository routeRepository;

    public RouteResource(RouteService routeService, RouteRepository routeRepository) {
        this.routeService = routeService;
        this.routeRepository = routeRepository;
    }

    /**
     * {@code POST  /routes} : Create a new route.
     *
     * @param routeDTO the routeDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new routeDTO, or with status {@code 400 (Bad Request)} if the route already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public Mono<ResponseEntity<RouteDTO>> createRoute(@Valid @RequestBody RouteDTO routeDTO) throws URISyntaxException {
        LOG.debug("REST request to save Route : {}", routeDTO);
        if (routeDTO.getId() != null) {
            throw new BadRequestAlertException("A new route cannot already have an ID", ENTITY_NAME, "idexists");
        }
        return routeService.save(routeDTO).map(result -> {
            try {
                return ResponseEntity.created(new URI("/api/routes/" + result.getId()))
                    .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
                    .body(result);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * {@code PUT  /routes/:id} : Updates an existing route.
     *
     * @param id the id of the routeDTO to save.
     * @param routeDTO the routeDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated routeDTO,
     * or with status {@code 400 (Bad Request)} if the routeDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the routeDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public Mono<ResponseEntity<RouteDTO>> updateRoute(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody RouteDTO routeDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Route : {}, {}", id, routeDTO);
        if (routeDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, routeDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        return routeRepository.existsById(id).flatMap(exists -> {
            if (!exists) {
                return Mono.error(new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));
            }

            return routeService
                .update(routeDTO)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                .map(result ->
                    ResponseEntity.ok()
                        .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
                        .body(result)
                );
        });
    }

    /**
     * {@code PATCH  /routes/:id} : Partial updates given fields of an existing route, field will ignore if it is null
     *
     * @param id the id of the routeDTO to save.
     * @param routeDTO the routeDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated routeDTO,
     * or with status {@code 400 (Bad Request)} if the routeDTO is not valid,
     * or with status {@code 404 (Not Found)} if the routeDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the routeDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public Mono<ResponseEntity<RouteDTO>> partialUpdateRoute(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody RouteDTO routeDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partially update Route : {}, {}", id, routeDTO);
        if (routeDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, routeDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        return routeRepository.existsById(id).flatMap(exists -> {
            if (!exists) {
                return Mono.error(new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));
            }

            Mono<RouteDTO> result = routeService.partialUpdate(routeDTO);

            return result.switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND))).map(res ->
                ResponseEntity.ok()
                    .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, res.getId().toString()))
                    .body(res)
            );
        });
    }

    /**
     * {@code GET  /routes} : get all the Routes.
     *
     * @param pageable the pagination information.
     * @param request a {@link ServerHttpRequest} request.
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Routes in body.
     */
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<List<RouteDTO>>> getAllRoutes(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        ServerHttpRequest request,
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get a page of Routes");
        return routeService
            .countAll()
            .zipWith(routeService.findAll(pageable).collectList())
            .map(countWithEntities ->
                ResponseEntity.ok()
                    .headers(
                        PaginationUtil.generatePaginationHttpHeaders(
                            ForwardedHeaderUtils.adaptFromForwardedHeaders(request.getURI(), request.getHeaders()),
                            new PageImpl<>(countWithEntities.getT2(), pageable, countWithEntities.getT1())
                        )
                    )
                    .body(countWithEntities.getT2())
            );
    }

    /**
     * {@code GET  /routes/:id} : get the "id" route.
     *
     * @param id the id of the routeDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the routeDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<RouteDTO>> getRoute(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Route : {}", id);
        Mono<RouteDTO> routeDTO = routeService.findOne(id);
        return ResponseUtil.wrapOrNotFound(routeDTO);
    }

    /**
     * {@code DELETE  /routes/:id} : delete the "id" route.
     *
     * @param id the id of the routeDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteRoute(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Route : {}", id);
        return routeService
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
