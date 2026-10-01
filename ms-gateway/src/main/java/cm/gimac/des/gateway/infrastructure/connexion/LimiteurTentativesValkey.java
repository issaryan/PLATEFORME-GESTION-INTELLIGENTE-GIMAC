package cm.gimac.des.gateway.infrastructure.connexion;

import cm.gimac.des.gateway.domain.connexion.port.LimiteurTentatives;
import cm.gimac.des.gateway.ratelimit.TokenBucketRateLimiter;
import java.util.List;
import reactor.core.publisher.Mono;

/**
 * Limitation partagée entre les instances : seau à jetons du TokenBucketRateLimiter de la
 * passerelle (script Lua atomique sur Valkey, gimac.passerelle.limitation-debit.actif). Si Valkey
 * ne répond pas (le limiteur laisse passer, solde -1), la limitation en mémoire prend le relais :
 * la connexion n'est jamais laissée sans limite.
 */
public class LimiteurTentativesValkey implements LimiteurTentatives {

    private final TokenBucketRateLimiter valkey;
    private final LimiteurTentatives secours;
    private final int capacite;
    private final int parMinute;

    public LimiteurTentativesValkey(TokenBucketRateLimiter valkey, LimiteurTentatives secours, int capacite, int parMinute) {
        this.valkey = valkey;
        this.secours = secours;
        this.capacite = capacite;
        this.parMinute = parMinute;
    }

    @Override
    public Mono<Boolean> consommer(String cle) {
        return valkey
            .isAllowed(cle, capacite, parMinute)
            .flatMap(r -> panne(r) ? secours.consommer(cle) : Mono.just(!r.isEmpty() && r.get(0) == 1L));
    }

    private static boolean panne(List<Long> r) {
        return r.size() > 1 && r.get(1) != null && r.get(1) == -1L;
    }
}
