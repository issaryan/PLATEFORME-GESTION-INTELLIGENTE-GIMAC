package cm.gimac.des.gateway.repository;

import cm.gimac.des.gateway.domain.PresentationLayer;
import cm.gimac.des.gateway.domain.Route;
import java.util.List;
import java.util.Map;
import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import reactor.core.publisher.Mono;

/**
 * Spring Data R2DBC custom repository implementation for the Route entity.
 */
@SuppressWarnings("unused")
class RouteRepositoryInternalImpl extends AbstractR2dbcRepository<Route, Long> implements RouteRepositoryInternal {

    private static final String SELECT_WITH_RELATIONS_SQL =
        "SELECT e.* FROM route e" + " LEFT JOIN presentation_layer e_layer ON e.layer_id = e_layer.id";

    // The clients sort by the related entity display field, with the entity id as secondary sort.
    private static final Map<String, String> SORT_COLUMNS = Map.ofEntries(
        Map.entry("id", "e.id"),
        Map.entry("layer.id", "e_layer.id"),
        Map.entry("layer.name", "e_layer.name")
    );

    public RouteRepositoryInternalImpl(R2dbcEntityTemplate template, R2dbcConverter converter, R2dbcDialect dialect) {
        super(Route.class, template, converter, dialect);
    }

    @Override
    protected String selectWithRelationsSql() {
        return SELECT_WITH_RELATIONS_SQL;
    }

    @Override
    protected Map<String, String> sortColumns() {
        return SORT_COLUMNS;
    }

    @Override
    protected Mono<List<Route>> populateRelationships(List<Route> entities) {
        return Mono.just(entities).flatMap(populated ->
            populate(populated, Route::getLayerId, PresentationLayer.class, PresentationLayer::getId, Route::setLayer)
        );
    }
}
