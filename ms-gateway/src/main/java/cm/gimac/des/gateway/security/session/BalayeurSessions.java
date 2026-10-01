package cm.gimac.des.gateway.security.session;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Balayage périodique des sessions (gimac.session.balayage, 1 min) : fermeture de celles qui sont échues. */
public class BalayeurSessions implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(BalayeurSessions.class);

    private final Disposable abonnement;

    public BalayeurSessions(FermetureSession fermeture, Duration periode) {
        this.abonnement = Flux.interval(periode, periode)
            .onBackpressureDrop()
            .concatMap(t -> fermeture.balayer().onErrorResume(e -> Mono.just(0)))
            .subscribe(n -> {
                if (n > 0) {
                    LOG.info("Sessions fermées après échéance : {}", n);
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
