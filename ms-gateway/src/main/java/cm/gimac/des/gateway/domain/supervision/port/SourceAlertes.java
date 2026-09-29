package cm.gimac.des.gateway.domain.supervision.port;

import cm.gimac.des.gateway.domain.supervision.Alerte;
import java.util.List;
import reactor.core.publisher.Mono;

/** Port sortant vers le gestionnaire d'alertes (Alertmanager) : alertes actives. */
public interface SourceAlertes {
    Mono<List<Alerte>> alertesActives();
}
