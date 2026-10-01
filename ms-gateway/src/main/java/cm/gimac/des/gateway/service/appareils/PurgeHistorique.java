package cm.gimac.des.gateway.service.appareils;

import cm.gimac.des.gateway.domain.appareils.port.HistoriqueConnexions;
import java.time.Clock;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Rétention de l'historique des connexions (90 jours, gimac.appareils.retention) : purge périodique. */
public class PurgeHistorique implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(PurgeHistorique.class);

    private final HistoriqueConnexions historique;
    private final Duration retention;
    private final Clock horloge;
    private final Disposable abonnement;

    public PurgeHistorique(HistoriqueConnexions historique, Duration retention, Duration periode, Clock horloge) {
        this.historique = historique;
        this.retention = retention;
        this.horloge = horloge;
        this.abonnement = Flux.interval(periode, periode)
            .onBackpressureDrop()
            .concatMap(t -> purger().onErrorResume(e -> {
                LOG.warn("Purge de l'historique des connexions impossible ({})", e.getMessage());
                return Mono.just(0L);
            }))
            .subscribe();
    }

    /** Supprime les tentatives plus anciennes que la rétention ; nombre supprimé. */
    public Mono<Long> purger() {
        return historique
            .purger(horloge.instant().minus(retention))
            .doOnNext(n -> {
                if (n > 0) {
                    LOG.info("Historique des connexions : {} tentatives de plus de {} jours supprimées", n, retention.toDays());
                }
            });
    }

    public boolean actif() {
        return !abonnement.isDisposed();
    }

    @Override
    public void close() {
        abonnement.dispose();
    }
}
