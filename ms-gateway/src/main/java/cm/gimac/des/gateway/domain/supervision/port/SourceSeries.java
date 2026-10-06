package cm.gimac.des.gateway.domain.supervision.port;

import cm.gimac.des.gateway.domain.supervision.SerieMetrique;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import reactor.core.publisher.Mono;

/** Historique des métriques (Prometheus, requête de plage) : séries brutes, sans densification. */
public interface SourceSeries {
    /**
     * @param promql requête issue de la liste fermée {@code MetriqueSerie}
     * @param etiquette étiquette qui nomme chaque série ; null : série unique nommée « plateforme »
     */
    Mono<List<SerieMetrique>> plage(String promql, String etiquette, Instant debut, Instant fin, Duration pas);
}
