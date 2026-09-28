package cm.gimac.des.gateway.service;

import cm.gimac.des.gateway.repository.RouteRepository;
import cm.gimac.des.gateway.service.dto.RouteDTO;
import cm.gimac.des.gateway.service.mapper.RouteMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service Implementation for managing {@link cm.gimac.des.gateway.domain.Route}.
 */
@Service
@Transactional
public class RouteService {

    private static final Logger LOG = LoggerFactory.getLogger(RouteService.class);

    private final RouteRepository routeRepository;

    private final RouteMapper routeMapper;

    public RouteService(RouteRepository routeRepository, RouteMapper routeMapper) {
        this.routeRepository = routeRepository;
        this.routeMapper = routeMapper;
    }

    /**
     * Save a route.
     *
     * @param routeDTO the entity to save.
     * @return the persisted entity.
     */
    public Mono<RouteDTO> save(RouteDTO routeDTO) {
        LOG.debug("Request to save Route : {}", routeDTO);
        return routeRepository.save(routeMapper.toEntity(routeDTO)).map(routeMapper::toDto);
    }

    /**
     * Update a route.
     *
     * @param routeDTO the entity to save.
     * @return the persisted entity.
     */
    public Mono<RouteDTO> update(RouteDTO routeDTO) {
        LOG.debug("Request to update Route : {}", routeDTO);
        return routeRepository.save(routeMapper.toEntity(routeDTO)).map(routeMapper::toDto);
    }

    /**
     * Partially update a route.
     *
     * @param routeDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Mono<RouteDTO> partialUpdate(RouteDTO routeDTO) {
        LOG.debug("Request to partially update Route : {}", routeDTO);

        return routeRepository
            .findById(routeDTO.getId())
            .map(existingRoute -> {
                routeMapper.partialUpdate(existingRoute, routeDTO);

                return existingRoute;
            })
            .flatMap(routeRepository::save)
            .map(routeMapper::toDto);
    }

    /**
     * Get all the routes.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Flux<RouteDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all Routes");
        return routeRepository.findAllBy(pageable).map(routeMapper::toDto);
    }

    /**
     * Get all the routes with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Flux<RouteDTO> findAllWithEagerRelationships(Pageable pageable) {
        return routeRepository.findAllWithEagerRelationships(pageable).map(routeMapper::toDto);
    }

    /**
     * Returns the number of routes available.
     * @return the number of entities in the database.
     *
     */
    public Mono<Long> countAll() {
        return routeRepository.count();
    }

    /**
     * Get one route by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Mono<RouteDTO> findOne(Long id) {
        LOG.debug("Request to get Route : {}", id);
        return routeRepository.findOneWithEagerRelationships(id).map(routeMapper::toDto);
    }

    /**
     * Delete the route by id.
     *
     * @param id the id of the entity.
     * @return a Mono to signal the deletion
     */
    public Mono<Void> delete(Long id) {
        LOG.debug("Request to delete Route : {}", id);
        return routeRepository.deleteById(id);
    }
}
