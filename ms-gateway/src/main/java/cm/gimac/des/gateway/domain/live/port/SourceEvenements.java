package cm.gimac.des.gateway.domain.live.port;

import cm.gimac.des.gateway.domain.live.EvenementLive;
import reactor.core.publisher.Flux;

/**
 * Port sortant vers les événements métier publiés par les services (sujets Kafka du lot 1) :
 * flux chaud, partagé par toutes les connexions de l'instance.
 */
@FunctionalInterface
public interface SourceEvenements {
    Flux<EvenementLive> evenements();
}
