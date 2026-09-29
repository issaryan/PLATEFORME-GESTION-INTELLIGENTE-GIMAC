package cm.gimac.des.gateway.domain.supervision.port;

import cm.gimac.des.gateway.domain.supervision.RouteVue;
import reactor.core.publisher.Flux;

/** Port sortant vers la table de routage effective de la passerelle. */
public interface SourceRoutes {
    Flux<RouteVue> routes();
}
