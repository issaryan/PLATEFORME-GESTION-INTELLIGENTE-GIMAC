package cm.gimac.des.gateway.infrastructure.supervision;

import cm.gimac.des.gateway.domain.supervision.MasqueSecrets;
import cm.gimac.des.gateway.domain.supervision.RouteVue;
import cm.gimac.des.gateway.domain.supervision.port.SourceRoutes;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import reactor.core.publisher.Flux;

/** Adaptateur du port {@link SourceRoutes} : routes effectives de Spring Cloud Gateway (RouteLocator). */
public class RoutesPasserelleAdapter implements SourceRoutes {

    private final RouteLocator routeLocator;

    public RoutesPasserelleAdapter(RouteLocator routeLocator) {
        this.routeLocator = routeLocator;
    }

    @Override
    public Flux<RouteVue> routes() {
        return routeLocator.getRoutes().map(RoutesPasserelleAdapter::vue);
    }

    static RouteVue vue(Route route) {
        return new RouteVue(
            route.getId(),
            MasqueSecrets.masquerValeur(String.valueOf(route.getUri())),
            MasqueSecrets.masquerValeur(String.valueOf(route.getPredicate())),
            route.getFilters().stream().map(RoutesPasserelleAdapter::filtre).toList(),
            route.getOrder()
        );
    }

    static String filtre(GatewayFilter filtre) {
        GatewayFilter f = filtre instanceof OrderedGatewayFilter o ? o.getDelegate() : filtre;
        return MasqueSecrets.masquerValeur(String.valueOf(f));
    }
}
