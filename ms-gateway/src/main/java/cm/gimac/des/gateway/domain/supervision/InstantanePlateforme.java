package cm.gimac.des.gateway.domain.supervision;

import java.time.Instant;
import java.util.List;

/** État de tous les services de la plateforme à un instant, et de Keycloak s'il est sondé (null sinon). */
public record InstantanePlateforme(Instant horodatage, EtatGlobal global, List<EtatService> services, EtatService keycloak) {
    public InstantanePlateforme {
        services = List.copyOf(services);
    }
}
