package cm.gimac.des.gateway.domain.supervision;

import java.time.Instant;
import java.util.List;

/** Configuration effective de la plateforme : routes de la passerelle et configuration de chaque service. */
public record ConfigurationPlateforme(Instant horodatage, List<RouteVue> routes, List<ConfigurationService> services) {
    public ConfigurationPlateforme {
        routes = List.copyOf(routes);
        services = List.copyOf(services);
    }
}
