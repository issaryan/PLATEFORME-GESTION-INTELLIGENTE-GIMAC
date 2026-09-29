package cm.gimac.des.gateway.domain.supervision.port;

import cm.gimac.des.gateway.domain.supervision.CibleCollecte;
import cm.gimac.des.gateway.domain.supervision.EtatConteneur;
import cm.gimac.des.gateway.domain.supervision.EtatHote;
import java.util.List;
import reactor.core.publisher.Mono;

/** Port sortant vers la base de métriques (Prometheus) : cibles de collecte, hôtes et conteneurs. */
public interface SourceServeurs {
    Mono<List<CibleCollecte>> cibles();

    /** Hôtes vus par node-exporter ; liste vide s'il n'est pas collecté. */
    Mono<List<EtatHote>> hotes();

    /** Conteneurs vus par cAdvisor ; liste vide s'il n'est pas collecté. */
    Mono<List<EtatConteneur>> conteneurs();
}
