package cm.gimac.des.gateway.repository;

import cm.gimac.des.gateway.domain.Route;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data R2DBC repository for the Route entity.
 */
@SuppressWarnings("unused")
@Repository
public interface RouteRepository extends ReactiveCrudRepository<Route, Long>, RouteRepositoryInternal {
    Flux<Route> findAllBy(Pageable pageable);

    @Override
    Mono<Route> findOneWithEagerRelationships(Long id);

    @Override
    Flux<Route> findAllWithEagerRelationships();

    @Override
    Flux<Route> findAllWithEagerRelationships(Pageable page);

    @Query("SELECT * FROM route entity WHERE entity.layer_id = :id")
    Flux<Route> findByLayer(Long id);

    @Query("SELECT * FROM route entity WHERE entity.layer_id IS NULL")
    Flux<Route> findAllWhereLayerIsNull();

    @Override
    <S extends Route> Mono<S> save(S entity);

    @Override
    Flux<Route> findAll();

    @Override
    Mono<Route> findById(Long id);

    @Override
    Mono<Void> deleteById(Long id);
}

interface RouteRepositoryInternal {
    Flux<Route> findAllBy(Pageable pageable);

    Flux<Route> findAll();

    Mono<Route> findById(Long id);

    Mono<Route> findOneWithEagerRelationships(Long id);

    Flux<Route> findAllWithEagerRelationships();

    Flux<Route> findAllWithEagerRelationships(Pageable page);
}
